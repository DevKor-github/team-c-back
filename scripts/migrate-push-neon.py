#!/usr/bin/env python3
"""Offline cutover copy. Defaults to read-only inventory. Never logs row/token contents."""
import argparse
import hashlib
import json
import datetime
import os
import re
from urllib.parse import urlparse, unquote

TABLES = {
    'tb_push_installation': 'push_installation_id',
    'tb_push_dispatch': 'push_dispatch_id',
    'tb_push_message': 'push_message_id',
    'tb_survey_push_schedule': 'survey_push_schedule_id',
}

def normalize(name):
    return name.replace('_', '').lower()

def convert_row(table, row, target_columns, pending_scope=None):
    fields = {normalize(key): value for key, value in row.items()}
    result = {}
    for column in target_columns:
        if normalize(column) in fields:
            result[column] = fields[normalize(column)]
        elif column == 'admin_only':
            result[column] = None
        elif column == 'audience':
            admin = fields.get('adminonly')
            result[column] = ('INTERNAL_TEST' if admin else 'LIVE') if admin is not None else 'LEGACY_UNKNOWN'
        else:
            raise ValueError(f'{table}: source missing required target column {column}')
    for column in ('active', 'admin_only'):
        if column in result and result[column] is not None:
            result[column] = bool(int.from_bytes(result[column], 'big')) if isinstance(result[column], bytes) else bool(result[column])
    if table == 'tb_survey_push_schedule' and result.get('status') == 'PENDING' and result.get('audience') == 'LEGACY_UNKNOWN':
        if not pending_scope:
            raise ValueError('Pending legacy schedules need an explicitly verified --pending-schedule-audience')
        result['audience'] = pending_scope
    return result

def row_bytes(values):
    return (json.dumps(values, default=lambda value: value.isoformat() if isinstance(value, (datetime.datetime, datetime.date)) else str(value),
        ensure_ascii=False, separators=(',', ':')) + '\n').encode('utf-8')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--writers-paused', action='store_true')
    parser.add_argument('--pending-schedule-audience', choices=['INTERNAL_TEST', 'LIVE'])
    args = parser.parse_args()
    if args.apply and not args.writers_paused:
        parser.error('--apply requires --writers-paused after stopping registration, producers and workers')
    import pymysql
    import psycopg
    from psycopg import sql
    source = urlparse(os.environ['PUSH_SOURCE_MYSQL_URL'])
    if source.scheme != 'mysql':
        raise ValueError('PUSH_SOURCE_MYSQL_URL must be a mysql:// URL')
    # Use the established TLS settings in a protected option file when required by the deployment.
    mysql = pymysql.connect(host=source.hostname, port=source.port or 3306,
        user=unquote(source.username or ''), password=unquote(source.password or ''),
        database=source.path.lstrip('/'), charset='utf8mb4', cursorclass=pymysql.cursors.SSDictCursor,
        ssl_ca=os.environ.get('PUSH_SOURCE_MYSQL_SSL_CA'))
    try:
        with mysql.cursor() as cursor, psycopg.connect(os.environ['PUSH_TARGET_POSTGRES_URL']) as pg:
            cursor.execute('SET TRANSACTION ISOLATION LEVEL REPEATABLE READ')
            cursor.execute('START TRANSACTION WITH CONSISTENT SNAPSHOT, READ ONLY')
            with pg.cursor() as target:
                if not args.apply:
                    target.execute('SET TRANSACTION READ ONLY')
                for table, identity in TABLES.items():
                    cursor.execute(f'SELECT COUNT(*) AS count FROM `{table}`')
                    source_count = cursor.fetchone()['count']
                    target.execute(sql.SQL('SELECT COUNT(*) FROM {}').format(sql.Identifier(table)))
                    target_count = target.fetchone()[0]
                    print(f'{table}: source={source_count}, target={target_count}')
                    if not args.apply:
                        continue
                    if target_count:
                        raise ValueError(f'{table}: target is not empty; refusing to overwrite or merge live data')
                    target.execute('SELECT column_name FROM information_schema.columns WHERE table_schema=current_schema() AND table_name=%s ORDER BY ordinal_position', (table,))
                    columns = [item[0] for item in target.fetchall()]
                    cursor.execute(f'SELECT * FROM `{table}` ORDER BY `{identity}`')
                    copied = 0
                    source_digest = hashlib.sha256()
                    statement = sql.SQL('INSERT INTO {} ({}) VALUES ({})').format(sql.Identifier(table),
                        sql.SQL(',').join(map(sql.Identifier, columns)), sql.SQL(',').join(sql.Placeholder() for _ in columns))
                    while rows := cursor.fetchmany(500):
                        values = [convert_row(table, row, columns, args.pending_schedule_audience) for row in rows]
                        ordered = [[row[column] for column in columns] for row in values]
                        for values_row in ordered: source_digest.update(row_bytes(values_row))
                        target.executemany(statement, ordered)
                        copied += len(rows)
                    target.execute(sql.SQL('SELECT COUNT(*) FROM {}').format(sql.Identifier(table)))
                    if target.fetchone()[0] != source_count or copied != source_count:
                        raise ValueError(f'{table}: count mismatch; rolling back')
                    target.execute(sql.SQL('SELECT {} FROM {} ORDER BY {}').format(sql.SQL(',').join(map(sql.Identifier, columns)), sql.Identifier(table), sql.Identifier(identity)))
                    target_digest = hashlib.sha256()
                    while imported := target.fetchmany(500):
                        for values_row in imported: target_digest.update(row_bytes(values_row))
                    if source_digest.digest() != target_digest.digest():
                        raise ValueError(f'{table}: row-content mismatch; rolling back')
                    print(f'{table}: verified {copied} rows including state and token content')
                    target.execute(sql.SQL("SELECT setval(pg_get_serial_sequence(%s,%s), COALESCE(MAX({}),1), COUNT(*)>0) FROM {}").format(sql.Identifier(identity),sql.Identifier(table)), (table,identity))
                if not args.apply:
                    pg.rollback()
                    print('Inventory only. No data changed.')
                else:
                    print('Copy verified. Target transaction will commit; old source tables remain untouched.')
    finally:
        mysql.rollback()
        mysql.close()

if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        # Drivers can include connection or row contents in exception messages.
        print(f'Migration stopped ({type(error).__name__}); target transaction rolled back. Inspect configuration/schema locally.')
        raise SystemExit(1)
