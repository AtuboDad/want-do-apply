#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""验证会员头像链路：换成 VIP 头像 → 别人能看到角标开关 → 关掉后不再展示。"""
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

API = "http://127.0.0.1:8080"
TAG = "vip" + str(int(time.time()))[-6:]


def call(method, path, token=None, body=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(API + path, data=data, headers=headers, method=method)
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    try:
        with opener.open(req, timeout=20) as r:
            return json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))


def login(dev, nick):
    r = call("POST", "/app/auth/login", body={"code": "dev:" + dev, "nickName": nick})
    return r["data"]["token"], r["data"]["user"]


ok = 0
fail = 0


def check(label, cond, extra=""):
    global ok, fail
    if cond:
        ok += 1
        print(f"  ✅ {label} {extra}")
    else:
        fail += 1
        print(f"  ❌ {label} {extra}")


# A 是会员（选 VIP 头像），B 是路人（要看 A 的角标）
tok_a, user_a = login(TAG + "a", "会员测试" + TAG)
tok_b, user_b = login(TAG + "b", "路人测试" + TAG)
print(f"A userId={user_a['id']}  B userId={user_b['id']}")

print("\n① 新用户默认 showVip")
me = call("GET", "/app/auth/me", tok_a)["data"]
check("默认展示角标", me.get("showVip") == "1", f"showVip={me.get('showVip')}")

print("\n② A 换成会员头像 av21")
r = call("PUT", "/app/auth/profile?avatar=" + urllib.parse.quote("preset:av21"), tok_a)
check("设置成功", r.get("code") == 200, r.get("msg", ""))
me = call("GET", "/app/auth/me", tok_a)["data"]
check("头像已存", me.get("avatar") == "preset:av21", me.get("avatar"))

print("\n③ A 发一条单，B 在广场看")
r = call("POST", "/app/order", tok_a, {
    "title": "会员角标验证单" + TAG,
    "price": 199,
    "reason": "这条单用来验证会员头像的角标会不会出现在别人的列表里，理由够五个字了。",
    "category": "other",
    "approveMode": "1",
    "durationHours": 24,
    "visibility": "0",
})
check("建单成功", r.get("code") == 200, r.get("msg", ""))
order_id = r.get("data")

sq = call("GET", "/app/order/square?pageNum=1&pageSize=20", tok_b)["data"]["rows"]
mine = [x for x in sq if str(x.get("id")) == str(order_id)]
check("B 能看到这单", len(mine) == 1)
if mine:
    check("列表带 userShowVip='1'", mine[0].get("userShowVip") == "1", mine[0].get("userShowVip"))
    check("列表头像为 av21", mine[0].get("userAvatar") == "preset:av21", mine[0].get("userAvatar"))

d = call("GET", f"/app/order/{order_id}", tok_b)["data"]["order"]
check("详情带 userShowVip='1'", d.get("userShowVip") == "1", d.get("userShowVip"))

print("\n④ A 把角标关掉")
r = call("PUT", "/app/auth/profile?showVip=0", tok_a)
check("关闭成功", r.get("code") == 200, r.get("msg", ""))
d2 = call("GET", f"/app/order/{order_id}", tok_b)["data"]["order"]
check("详情里开关变 '0'（前端据此隐藏角标）", d2.get("userShowVip") == "0", d2.get("userShowVip"))
sq2 = call("GET", "/app/order/square?pageNum=1&pageSize=20", tok_b)["data"]["rows"]
m2 = [x for x in sq2 if str(x.get("id")) == str(order_id)]
if m2:
    check("列表里开关也是 '0'", m2[0].get("userShowVip") == "0", m2[0].get("userShowVip"))

print("\n⑤ 头像本身没被改掉（关角标≠卸会员）")
me = call("GET", "/app/auth/me", tok_a)["data"]
check("头像仍是 av21", me.get("avatar") == "preset:av21", me.get("avatar"))

print("\n⑥ 非法开关值要被拦住")
r = call("PUT", "/app/auth/profile?showVip=2", tok_a)
check("showVip=2 被拒", r.get("code") != 200, f"code={r.get('code')} {r.get('msg','')}")

print("\n⑦ 会员头像能存（av26）")
r = call("PUT", "/app/auth/profile?avatar=" + urllib.parse.quote("preset:av26"), tok_a)
check("av26 可保存", r.get("code") == 200, r.get("msg", ""))

print(f"\n结果：{ok} 通过 / {fail} 失败")
sys.exit(0 if fail == 0 else 1)
