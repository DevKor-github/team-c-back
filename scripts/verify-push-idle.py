#!/usr/bin/env python3
"""Read-only comparison of push polling counters; never prints connection credentials or rows."""
import datetime
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import urllib.parse


def main():
    if importlib.util.find_spec("psycopg2") is None:
        # Reuse the migration's existing protected environment; install nothing on the host.
        for candidate in Path("/home/yjlee/infra/neon-cutover-20260928").glob("**/bin/python*"):
            result = subprocess.run([str(candidate), "-c", "import psycopg2"], capture_output=True)
            if result.returncode == 0:
                os.execv(str(candidate), [str(candidate), __file__])
        print("Postgres counter check unavailable: existing migration Python driver not found")
        return
    import psycopg2
    container = json.loads(subprocess.check_output(["docker", "inspect", "app-latest"], text=True))[0]
    environment = dict(value.split("=", 1) for value in container["Config"]["Env"])
    url = urllib.parse.urlsplit(environment["PUSH_DATABASE_URL"].removeprefix("jdbc:"))
    connection = psycopg2.connect(host=url.hostname, port=url.port or 5432, dbname=url.path.lstrip("/"),
            user=environment.get("PUSH_DATABASE_USERNAME") or url.username,
            password=environment.get("PUSH_DATABASE_PASSWORD") or url.password,
            sslmode="require", connect_timeout=10)
    connection.autocommit = True
    connection.set_session(readonly=True)

    def snapshot():
        with connection.cursor() as cursor:
            cursor.execute("""SELECT queryid, calls, query FROM pg_stat_statements
                    WHERE userid = (SELECT oid FROM pg_roles WHERE rolname = current_user)
                    AND (query ILIKE %s OR query ILIKE %s) AND query ILIKE %s""",
                    ("%FROM tb_push_message%", "%FROM tb_survey_push_schedule%", "%SKIP LOCKED%"))
            totals = {"dispatch": 0, "receipt": 0, "survey": 0, "recovery": 0}
            for query_id, calls, query in cursor.fetchall():
                text = " ".join(query.lower().split())
                kind = "survey" if "tb_survey_push_schedule" in text else "receipt" if "expo_ticket_id is not null" in text else "dispatch" if "next_retry_at is null" in text else "recovery"
                totals[kind] += calls
            return totals

    try:
        first = snapshot()
        started = datetime.datetime.now(datetime.timezone.utc).isoformat()
        time.sleep(30)
        second = snapshot()
        print(json.dumps({"started_at": started, "interval_seconds": 30, "before": first, "after": second,
                          "delta": {key: second[key] - first[key] for key in first}}), flush=True)
    finally:
        connection.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        # Database/driver exception messages can contain connection details.
        print("Postgres counter check unavailable:", type(error).__name__)
        sys.exit(1)
