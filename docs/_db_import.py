"""
建库 + 导入 SQL
用法: python _db_import.py [库名] [sql文件...]
连接参数从环境变量或 docs/.env 读取（见 _env.py）。
"""
import os
import sys

import pymysql

import _env

CONF = _env.mysql_conf()

HOST = CONF["host"]
PORT = CONF["port"]
USER = CONF["user"]
PWD = CONF["password"]


def split_statements(path):
    """按行切分 SQL：跳过注释行，遇到行尾分号视为语句结束。"""
    stmts, buf = [], []
    with open(path, "r", encoding="utf-8-sig") as f:
        for raw in f:
            line = raw.rstrip("\n")
            stripped = line.strip()
            if not stripped or stripped.startswith("--") or stripped.startswith("#"):
                continue
            buf.append(line)
            if stripped.endswith(";"):
                stmts.append("\n".join(buf).rstrip(";").strip())
                buf = []
    if buf:
        stmts.append("\n".join(buf).strip())
    return [s for s in stmts if s]


def main():
    db = sys.argv[1]
    files = sys.argv[2:]

    conn = pymysql.connect(host=HOST, port=PORT, user=USER, password=PWD,
                           charset="utf8mb4", autocommit=True)
    with conn.cursor() as cur:
        cur.execute(
            f"create database if not exists `{db}` "
            "default character set utf8mb4 collate utf8mb4_general_ci"
        )
        print(f"[ok] database `{db}` ready")
    conn.close()

    conn = pymysql.connect(host=HOST, port=PORT, user=USER, password=PWD,
                           database=db, charset="utf8mb4", autocommit=True)
    ok = fail = 0
    with conn.cursor() as cur:
        for path in files:
            stmts = split_statements(path)
            print(f"[run] {os.path.basename(path)} -> {len(stmts)} statements")
            for s in stmts:
                try:
                    cur.execute(s)
                    ok += 1
                except Exception as e:
                    fail += 1
                    head = " ".join(s.split())[:110]
                    print(f"  [FAIL] {head}\n         {e}")
    conn.close()
    print(f"[done] ok={ok} fail={fail}")
    sys.exit(1 if fail else 0)


if __name__ == "__main__":
    main()
