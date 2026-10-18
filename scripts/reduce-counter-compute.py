#!/usr/bin/env python3
"""Preserve the request counter's durable SQLite state while adding an hourly checkpoint."""
import base64
import datetime
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import sqlite3
import subprocess
import sys
import time

ROOT = Path('/home/yjlee/infra/request-counter')
BACKUPS = Path('/home/yjlee/infra/counter-compute-20261018')


def main():
    if importlib.util.find_spec('psycopg') is None:
        for python in Path('/home/yjlee/infra/neon-cutover-20260928').glob('**/bin/python'):
            os.execv(str(python), [str(python), __file__, *sys.argv[1:]])
        raise RuntimeError('Existing Postgres driver not found')
    import psycopg
    os.umask(0o077)
    phase = sys.argv[1]
    BACKUPS.mkdir(mode=0o700, exist_ok=True)
    payloadfile = BACKUPS / 'payload.json'
    if os.environ.get('COUNTER_COMPUTE_PAYLOAD'):
        payloadfile.write_bytes(base64.b64decode(os.environ.pop('COUNTER_COMPUTE_PAYLOAD')))
        payloadfile.chmod(0o600)
    payload = json.loads(payloadfile.read_text())
    if phase == 'apply':
        backup = BACKUPS / datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
        backup.mkdir(mode=0o700)
        script = ROOT / 'collect.py'
        original_stat = script.stat()
        db_stat = (ROOT / 'actions.sqlite').stat()
        (backup / 'collect-before.py').write_bytes(script.read_bytes())
        subprocess.run(['systemctl', 'stop', 'kodaero-request-counter'], check=True)
        # Allow an already accepted legacy HTTP request to finish before seeding.
        time.sleep(15)
        db = sqlite3.connect(ROOT / 'actions.sqlite')
        with sqlite3.connect(backup / 'actions-before.sqlite') as saved:
            db.backup(saved)
        candidate = backup / 'collect-next.py'
        candidate.write_bytes(base64.b64decode(payload['collector']))
        compile(candidate.read_text(), str(candidate), 'exec')
        spec = importlib.util.spec_from_file_location('counter_next', candidate)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        module.setup_ledger(db)
        initialized = db.execute("SELECT 1 FROM live_settings WHERE key='generation'").fetchone()
        if not initialized:
            with psycopg.connect(payload['database_url']) as c:
                c.execute('SET TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY')
                windows = [row[0] for row in c.execute('SELECT key FROM koyeon_live.action_windows')]
                counters = {key.rsplit(':',1)[-1]: {'total': int(total), 'updated_at': at.isoformat()}
                            for key,total,at in c.execute("SELECT key,total,updated_at FROM koyeon_live.counters WHERE key IN ('logged-user-actions-2026:production','logged-user-actions-2026:development')")}
            module.seed_ledger(db, windows, counters)
        cachefile = ROOT / 'cache.secret.json'
        cachefile.write_text(json.dumps(payload['cache']))
        cachefile.chmod(0o600)
        os.chown(cachefile, original_stat.st_uid, original_stat.st_gid)
        module.publish_snapshot(db, payload['cache'])
        db.close()
        for path in ROOT.glob('actions.sqlite*'):
            os.chown(path, db_stat.st_uid, db_stat.st_gid)
        script.write_bytes(candidate.read_bytes())
        script.chmod(original_stat.st_mode & 0o777)
        os.chown(script, original_stat.st_uid, original_stat.st_gid)
        subprocess.run(['systemctl', 'start', 'kodaero-request-counter'], check=True)
        print('Collector updated; state, identity, ingest secret and offsets preserved', flush=True)
        print('backup', str(backup), 'code_sha256', hashlib.sha256(script.read_bytes()).hexdigest(), flush=True)
    elif phase != 'audit':
        raise ValueError('Unknown phase')
    print('service', subprocess.check_output(['systemctl', 'is-active', 'kodaero-request-counter'], text=True).strip(), flush=True)
    with sqlite3.connect(f'file:{ROOT}/actions.sqlite?mode=ro', uri=True) as db:
        print('pending_batches', db.execute('SELECT count(*) FROM pending').fetchone()[0], flush=True)
        if db.execute("SELECT 1 FROM sqlite_master WHERE name='live_counters'").fetchone():
            print('live_counts', db.execute('SELECT channel,total FROM live_counters ORDER BY channel').fetchall(), flush=True)
            print('checkpoint_hour', db.execute("SELECT value FROM live_settings WHERE key='checkpoint_hour'").fetchone(), flush=True)
    with psycopg.connect(payload['database_url']) as c:
        c.execute('SET TRANSACTION READ ONLY')
        print('neon_counts', [(k.rsplit(':',1)[-1],int(n)) for k,n in c.execute("SELECT key,total FROM koyeon_live.counters WHERE key IN ('logged-user-actions-2026:production','logged-user-actions-2026:development') ORDER BY key")], flush=True)


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print('Counter operation stopped:', type(error).__name__, flush=True)
        sys.exit(1)
