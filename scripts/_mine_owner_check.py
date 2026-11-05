#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""验证「我的 - 我提交的」到底按谁的 user_id 查。

关键结论要证明三件事：
① 「我的」列表不靠前端传 userId，后端从 App-Token（JWT）里取；
② A 建的单只出现在 A 的列表里，B 查不到；
③ 前端就算硬塞 userId 参数，也不会改变查询结果（防越权）。
"""
import json
import time
import urllib.request

BASE = "http://127.0.0.1:8080"


def call(method, path, body=None, token=None):
    url = BASE + path
    data = None
    headers = {"Content-Type": "application/json"}
    if body is not None:
        data = json.dumps(body).encode()
    if token:
        headers["App-Token"] = token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw or "{}")
        except Exception:
            return e.code, {"raw": raw}


def login(tag):
    st, r = call("POST", "/app/auth/login", {"code": "dev:" + tag})
    if st != 200 or r.get("code") != 200:
        raise SystemExit(f"登录失败 {tag}: {st} {r}")
    d = r["data"]
    return d["token"], d["userInfo"] if "userInfo" in d else d.get("user")


def main():
    suffix = str(int(time.time()))[-5:]
    ta, ua = login("owner-a-" + suffix)
    tb, ub = login("owner-b-" + suffix)
    print(f"A: id={ua.get('id')} 昵称={ua.get('nickName')}")
    print(f"B: id={ub.get('id')} 昵称={ub.get('nickName')}")
    assert str(ua.get("id")) != str(ub.get("id")), "两个账号被合并成同一个用户了"

    print("\n① A 连发 2 单")
    ids = []
    for i in (1, 2):
        st, r = call("POST", "/app/order", {
            "title": f"归属校验-{suffix}-{i}",
            "price": 19 + i,
            "reason": "验证我的列表只认登录态里的 user_id，不看前端传参",
            "category": "other",
            "visibility": "0",
            "approveMode": "0",
            "durationHours": 24,
        }, ta)
        print("   ", st, r.get("code"), r.get("msg"))
        if r.get("code") == 200:
            ids.append(r["data"])
    print("   新建单 id:", ids)

    print("\n② A 查「我提交的」")
    st, r = call("GET", f"/app/order/mine?type=apply&pageNum=1&pageSize=10", token=ta)
    rows_a = r.get("data", {}).get("rows", [])
    titles_a = [x["title"] for x in rows_a]
    print(f"   共 {len(rows_a)} 条:", titles_a[:5])
    hit = [t for t in titles_a if suffix in str(t)]
    print("   本轮新建的:", hit, "✅" if len(hit) == len(ids) else "❌")
    owners = {(x.get("userNickName"), str(x.get("userId"))) for x in rows_a}
    print("   列表里的作者字段:", list(owners)[:3])

    print("\n③ B 查「我提交的」")
    st, r = call("GET", "/app/order/mine?type=apply&pageNum=1&pageSize=10", token=tb)
    rows_b = r.get("data", {}).get("rows", [])
    leaked = [x["title"] for x in rows_b if suffix in str(x["title"])]
    print(f"   B 共 {len(rows_b)} 条，看到 A 的单:", leaked)
    print("   隔离:", "✅ 没串" if not leaked else "❌ 串数据了")

    print("\n④ 不登录直接查")
    st, r = call("GET", "/app/order/mine?type=apply")
    print(f"   HTTP {st} msg={r.get('msg')}", "✅" if st == 401 or r.get("code") == 401 else "❌")

    print("\n⑤ 拿 B 的 token，硬塞 A 的 userId 参数")
    st, r = call(
        "GET",
        f"/app/order/mine?type=apply&pageNum=1&pageSize=10&userId={ua.get('id')}&user_id={ua.get('id')}",
        token=tb,
    )
    rows_b2 = r.get("data", {}).get("rows", [])
    leaked2 = [x["title"] for x in rows_b2 if suffix in str(x["title"])]
    print(f"   返回 {len(rows_b2)} 条，看到 A 的单:", leaked2)
    print("   传参无效（仍按 token 认人）:", "✅" if not leaked2 else "❌ 参数生效了，有越权风险")

    print("\n⑥ 数据核对：详情里的 userId / mine 标记")
    print("   本轮 A 的单 id:", ids, " 期望 user_id =", ua.get("id"))
    for oid in ids:
        st, r = call("GET", f"/app/order/{oid}", token=ta)
        # 详情返回的是 { order: {...}, votes: [], comments: [] }
        d = (r.get("data") or {}).get("order") or {}
        same = str(d.get("userId")) == str(ua.get("id"))
        print(f"   单 {oid}: userId={d.get('userId')} mine={d.get('mine')}",
              "✅" if same and d.get("mine") is True else "❌")
    # 再去掉 token 看一次，确认别人的单 mine=false 而不是报错
    if ids:
        st, r = call("GET", f"/app/order/{ids[0]}", token=tb)
        d = (r.get("data") or {}).get("order") or {}
        print(f"   B 看 A 的单: userId={d.get('userId')} mine={d.get('mine')}",
              "✅" if d.get("mine") is False else "❌")


if __name__ == "__main__":
    main()
