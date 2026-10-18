#!/usr/bin/env python3
"""Stage or activate an already restored Neon database without rebuilding the backend.

Credentials arrive through a temporary GitHub secret, never through CLI arguments.
The source database must be write-frozen and final checksums verified before apply.
"""
import base64
import datetime
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import urllib.parse

ROOT = Path('/home/yjlee/infra')
DIRECTORY = ROOT / 'neon-rehome-20261018'
TARGETS = ('app-dev', 'app-latest', 'app-stable')
EXPECTED_IMAGE = 'sha256:383f62278aa4be6717ff8f082d18002f916560e7ee7c439436cbf53955dae0ec'


def run(*args):
    return subprocess.check_output(args, text=True)


def inspect(name):
    return json.loads(run('docker', 'inspect', name))[0]


def private(path, content):
    path.write_text(content)
    path.chmod(0o600)


def health(name, port=8080):
    for _ in range(50):
        try:
            output = run('docker', 'exec', name, 'wget', '-q', '-O', '-',
                         f'http://127.0.0.1:{port}/actuator/health')
            if json.loads(output).get('status') == 'UP':
                return True
        except (subprocess.CalledProcessError, json.JSONDecodeError):
            pass
        time.sleep(3)
    return False


def configuration():
    return json.loads((DIRECTORY / 'target.json').read_text())


def main():
    phase = sys.argv[1]
    os.umask(0o077)
    os.chdir(ROOT)
    DIRECTORY.mkdir(mode=0o700, exist_ok=True)
    if phase == 'prepare':
        if (DIRECTORY / 'receipt.json').exists():
            raise RuntimeError('Cutover already completed')
        payload = json.loads(base64.b64decode(os.environ.pop('NEON_REHOME_PAYLOAD')))
        parsed = urllib.parse.urlsplit(payload['PUSH_DATABASE_URL'].removeprefix('jdbc:'))
        if parsed.hostname != 'ep-autumn-sky-b8q8x7hb-pooler.c-14.us-east-1.aws.neon.tech':
            raise RuntimeError('Unexpected target database')
        originals = {name: inspect(name) for name in TARGETS}
        if any(c['Image'] != EXPECTED_IMAGE for c in originals.values()):
            raise RuntimeError('Backend image changed since review')
        if not (DIRECTORY / 'containers-before.json').exists():
            private(DIRECTORY / 'containers-before.json', json.dumps(originals))
            private(DIRECTORY / 'neon-push-before.env', (ROOT / 'neon-push.env').read_text())
        private(DIRECTORY / 'target.json', json.dumps(payload))
        environment = dict(v.split('=', 1) for v in originals['app-dev']['Config']['Env'])
        environment.update(payload)
        environment.update({'SERVER_PORT': '18099', 'MANAGEMENT_SERVER_PORT': '18099',
                            'PUSH_WORKER_ENABLED': 'false', 'PUSH_RECEIPT_WORKER_ENABLED': 'false',
                            'PUSH_SURVEY_WORKER_ENABLED': 'false', 'PUSH_OUTBOX_ENABLED': 'false',
                            'PUSH_RECOVERY_WORKER_ENABLED': 'false'})
        envfile = DIRECTORY / 'canary.env'
        if any('\n' in v for v in environment.values()):
            raise ValueError('Multiline environment')
        private(envfile, '\n'.join(k + '=' + v for k, v in environment.items()) + '\n')
        canary = 'kodaero-neon-rehome-canary'
        try:
            run('docker', 'run', '--detach', '--name', canary, '--network',
                originals['app-dev']['HostConfig']['NetworkMode'], '--env-file', str(envfile),
                '--entrypoint', 'java', EXPECTED_IMAGE, '-Xmx768m', '-Xms128m',
                '-XX:MaxMetaspaceSize=256m', '-jar', '/app.jar', '--server.port=18099',
                '--management.server.port=18099')
            if not health(canary, 18099):
                logs = subprocess.run(['docker', 'logs', canary], text=True, capture_output=True)
                private(DIRECTORY / 'canary-failure.log', logs.stdout + logs.stderr)
                raise RuntimeError('Target canary failed; live backends unchanged')
        finally:
            subprocess.run(['docker', 'rm', '--force', canary], capture_output=True)
            envfile.unlink(missing_ok=True)
        private(DIRECTORY / 'prepared.json', json.dumps({'image_id': EXPECTED_IMAGE, 'canary': 'UP'}))
        print('Target canary UP; live backends unchanged', flush=True)
    elif phase == 'apply':
        if not (DIRECTORY / 'prepared.json').exists() or (DIRECTORY / 'receipt.json').exists():
            raise RuntimeError('Prepare required; refusing duplicate activation')
        originals = json.loads((DIRECTORY / 'containers-before.json').read_text())
        if any(inspect(name)['Image'] != EXPECTED_IMAGE for name in TARGETS):
            raise RuntimeError('Backend image changed since prepare')
        payload = configuration()
        # Persist for standard future deployments as well as this frozen rollout.
        envfile = ROOT / 'neon-push.env'
        lines = [line for line in envfile.read_text().splitlines()
                 if line.split('=', 1)[0] not in payload]
        staging = DIRECTORY / 'neon-push-next.env'
        private(staging, '\n'.join(lines + [k + '=' + v for k, v in payload.items()]) + '\n')
        staging.replace(envfile)
        override = DIRECTORY / 'compose.json'
        private(override, json.dumps({'services': {name: {'image': EXPECTED_IMAGE,
                    'environment': payload} for name in TARGETS}}))
        command = ['docker', 'compose']
        for path in originals['app-dev']['Config']['Labels']['com.docker.compose.project.config_files'].split(','):
            command.extend(['-f', path])
        command.extend(['-f', str(override)])
        # All old consumers stop before any new one starts. Never run both DBs' workers.
        run('docker', 'stop', *TARGETS)
        for name in TARGETS:
            run(*command, 'up', '-d', '--no-deps', '--force-recreate', name)
            if not health(name):
                raise RuntimeError('Target health failed; source remains frozen; inspect protected backup')
            print(name, 'UP on target database', flush=True)
        receipt = {'target_project': 'lively-shape-96540511', 'image_id': EXPECTED_IMAGE,
                   'containers': list(TARGETS), 'completed_at': datetime.datetime.now(datetime.timezone.utc).isoformat()}
        private(DIRECTORY / 'receipt.json', json.dumps(receipt, indent=2))
        print(json.dumps(receipt), flush=True)
    elif phase == 'verify':
        payload = configuration()
        for name in TARGETS:
            c = inspect(name)
            environment = dict(v.split('=', 1) for v in c['Config']['Env'])
            if c['Image'] != EXPECTED_IMAGE or any(environment.get(k) != v for k, v in payload.items()):
                raise RuntimeError('Container configuration mismatch: ' + name)
            if not health(name):
                raise RuntimeError('Container health failed: ' + name)
            print(name, 'target config matches', 'health', 'UP',
                  'audience', environment.get('PUSH_AUDIENCE'), flush=True)
        print((DIRECTORY / 'receipt.json').read_text(), flush=True)
    else:
        raise ValueError('Unknown phase')


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        # Exception messages may contain connection strings from third-party libraries.
        print('Neon rehome stopped:', type(error).__name__, flush=True)
        sys.exit(1)
