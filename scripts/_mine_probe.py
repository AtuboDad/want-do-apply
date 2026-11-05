#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""用真实用户 id 直接签一个小程序 token，验证「我提交的」能不能查出来。

不经过微信登录，绕开 openid / mock 那一层，只看后端查询本身对不对。
用法：python scripts/_mine_probe.py <user_id>
"""
import base64
import hashlib
import hmac
import json
import sys
import time
import urllib.parse
import urllib.request

BASE = "http://127.0.0.1:8080"
SECRET = "want-do-apply-2026-change-me-in-production"


def b64(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).decode().rstrip("=")


def make_token(uid: int) -> str:
    now = int(time.time())
    header = {"alg": "HS256", "typ": "JWT"}
    payload = {"uid": uid, "iat": now, "exp": now + 7 * 24 * 3600}
    body = b64(json.dumps(header, separators=(",", ":")).encode()) + "." + \
           b64(json.dumps(payload, separators=(",", ":")).encode())
    sig = hmac.new(SECRET.encode(), body.encode(), hashlib.sha256).digest()
    return body + "." + b64(sig)


def call(path, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["App-Token"] = token
    req = urllib.request.Request(BASE + path, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode() or "{}")


def main():
    uid = int(sys.argv[1]) if len(sys.argv) > 1 else 2118155499349266434
    token = make_token(uid)
    print("uid =", uid)
    print("token =", token[:36] + "...")

    st, r = call(f"/app/order/mine?type=apply&pageNum=1&pageSize=10", token)
    print("\n[1] /app/order/mine?type=apply")
    print("    HTTP", st, "code", r.get("code"), "msg", r.get("msg"))
    data = r.get("data") or {}
    rows = data.get("rows") or []
    print("    total =", data.get("total"), " 本页 =", len(rows))
    for x in rows:
        print(f"    - {x.get('id')} | {x.get('title')} | userId={x.get('userId')} status={x.get('status')} mine={x.get('mine')}")

    st, r = call("/app/user/profile", token)
    p = r.get("data") or {}
    u = p.get("user") or {}
    print("\n[2] /app/user/profile")
    print(f"    id={u.get('id')} 昵称={u.get('nickName')} 发起={p.get('applyTotal')} 处理={p.get('voteTotal')}")

    print("\n[3] 每个状态单独查")
    for s in ["0", "1", "2", "4", "3"]:
        st, r = call(f"/app/order/mine?type=apply&pageNum=1&pageSize=10&status={s}", token)
        d = r.get("data") or {}
        print(f"    status={s}: total={d.get('total')}")

    print("\n[4] 带关键字的查询")
    kw = urllib.parse.quote("switch")
    st, r = call(f"/app/order/mine?type=apply&pageNum=1&pageSize=10&keyword={kw}", token)
    d = r.get("data") or {}
    print(f"    keyword=switch: total={d.get('total')}")


if __name__ == "__main__":
    main()
