#!/usr/bin/env python3
"""Deploy one tested immutable backend to all push workers without changing data or secrets."""
import datetime
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time


def run(*args, capture=False):
    return subprocess.run(args, check=True, text=True, stdout=subprocess.PIPE if capture else None).stdout


def inspect(name):
    return json.loads(run("docker", "inspect", name, capture=True))[0]


def health(name, port=8080, timeout=150):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if not inspect(name)["State"]["Running"]:
            return False
        try:
            result = subprocess.run(["docker", "exec", name, "wget", "-q", "-O", "-",
                                     f"http://127.0.0.1:{port}/actuator/health"],
                                    text=True, capture_output=True, timeout=15)
        except subprocess.TimeoutExpired:
            time.sleep(3)
            continue
        try:
            if result.returncode == 0 and json.loads(result.stdout).get("status") == "UP":
                return True
        except json.JSONDecodeError:
            pass
        time.sleep(3)
    return False


def save_private(path, data):
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(descriptor, "w") as stream:
        stream.write(data)


def main():
    image, revision = sys.argv[1:]
    if not re.fullmatch(r"[0-9a-f]{40}", revision) or not re.fullmatch(r"[\w./-]+:neon-" + revision, image):
        raise ValueError("An immutable neon-<commit> image is required")
    os.chdir("/home/yjlee/infra")
    os.umask(0o077)
    targets = ["app-dev", "app-latest", "app-stable"]
    originals = {name: inspect(name) for name in targets}
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    backup = Path("push-idle-rollouts") / (stamp + "-" + revision[:12])
    backup.mkdir(parents=True, mode=0o700)
    save_private(backup / "containers.json", json.dumps(originals))
    run("docker", "pull", image)
    metadata = inspect(image)
    if metadata["Config"].get("Labels", {}).get("com.kodaero.push-idle") != "wake-v1":
        raise ValueError("Refusing image without the wake-on-work implementation")
    image_id = metadata["Id"]
    for name, original in originals.items():
        run("docker", "tag", original["Image"], f"kodaero-backup:{name}-pre-idle-{stamp}")

    # Validate both database configurations on a separate instance before stopping any live backend.
    canary = "kodaero-push-idle-canary-" + revision[:12]
    original = originals["app-dev"]
    environment = dict(value.split("=", 1) for value in original["Config"]["Env"])
    environment.update({"SERVER_PORT": "18099", "MANAGEMENT_SERVER_PORT": "18099",
                        "PUSH_WORKER_ENABLED": "false", "PUSH_RECEIPT_WORKER_ENABLED": "false",
                        "PUSH_SURVEY_WORKER_ENABLED": "false", "PUSH_OUTBOX_ENABLED": "false",
                        "PUSH_RECOVERY_WORKER_ENABLED": "false"})
    env_file = backup / "canary.env"
    if any("\n" in value for value in environment.values()):
        raise ValueError("Multiline environment requires a separately reviewed canary configuration")
    save_private(env_file, "\n".join(key + "=" + value for key, value in environment.items()) + "\n")
    try:
        run("docker", "run", "--detach", "--name", canary, "--network", original["HostConfig"]["NetworkMode"],
            "--env-file", str(env_file), "--entrypoint", "java", image,
            "-Xmx768m", "-Xms128m", "-XX:MaxMetaspaceSize=256m", "-jar", "/app.jar",
            "--server.port=18099", "--management.server.port=18099")
        if not health(canary, 18099):
            logs = subprocess.run(["docker", "logs", canary], text=True, capture_output=True, check=True)
            save_private(backup / "canary-failure.log", logs.stdout + logs.stderr)
            raise RuntimeError("Canary failed; live containers were not modified. Protected logs: " + str(backup))
        print("Canary UP; database validation completed; no push workers enabled", flush=True)
    finally:
        subprocess.run(["docker", "rm", "--force", canary], check=False)
        env_file.unlink(missing_ok=True)

    compose_paths = originals["app-dev"]["Config"]["Labels"]["com.docker.compose.project.config_files"].split(",")
    command = ["docker", "compose"]
    for path in compose_paths:
        command.extend(["-f", path])
    override = backup / "compose.json"
    save_private(override, json.dumps({"services": {name: {"image": image} for name in targets}}))
    rollout = command + ["-f", str(override)]
    updated = []
    for name in targets:
        try:
            run(*rollout, "up", "-d", "--no-deps", "--force-recreate", name)
            if inspect(name)["Image"] != image_id or not health(name):
                raise RuntimeError("New container failed health: " + name)
            updated.append(name)
            print(name, "UP", image_id, flush=True)
        except Exception:
            # Restore only this container's prior immutable image. Leave verified new peers running.
            rollback = backup / (name + "-rollback.json")
            save_private(rollback, json.dumps({"services": {name: {"image": originals[name]["Image"]}}}))
            run(*command, "-f", str(rollback), "up", "-d", "--no-deps", "--force-recreate", name)
            print("Partial rollout; updated:", updated, "; restored:", name, flush=True)
            raise

    receipt = {"revision": revision, "image": image, "image_id": image_id, "containers": updated,
               "backup": str(backup), "completed_at": datetime.datetime.now(datetime.timezone.utc).isoformat()}
    save_private(backup / "receipt.json", json.dumps(receipt, indent=2))
    print(json.dumps(receipt), flush=True)


if __name__ == "__main__":
    main()
