"""MySQL 连通性探活

连接参数从环境变量或 docs/.env 读取（见 _env.py）。
"""
import sys

import pymysql

import _env

conf = _env.mysql_conf()
try:
    conn = pymysql.connect(connect_timeout=8, **conf)
except Exception as e:
    print("CONNECT_FAIL:", type(e).__name__, e)
    sys.exit(1)

with conn.cursor() as cur:
    cur.execute("select version()")
    print("MYSQL_VERSION:", cur.fetchone()[0])
    cur.execute("select current_user()")
    print("CURRENT_USER:", cur.fetchone()[0])
    cur.execute("show grants")
    for row in cur.fetchall():
        print("GRANT:", row[0])
    cur.execute("show databases")
    print("DATABASES:", [r[0] for r in cur.fetchall()])
conn.close()
