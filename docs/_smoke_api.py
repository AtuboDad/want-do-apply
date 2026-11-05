"""钱包请等等 - 后端接口冒烟测试"""
import json
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:8080"
ok = 0
fail = 0


def call(method, path, body=None, token=None):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("App-Token", token)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())


def check(name, cond, extra=""):
    global ok, fail
    if cond:
        ok += 1
        print(f"  [PASS] {name} {extra}")
    else:
        fail += 1
        print(f"  [FAIL] {name} {extra}")


print("== 1. 登录 ==")
r = call("POST", "/app/auth/login", {"code": "dev:alice", "nickName": "爱丽丝", "avatar": ""})
check("alice login", r.get("code") == 200, r.get("msg", ""))
alice = r["data"]["token"]
alice_id = r["data"]["user"]["id"]

r = call("POST", "/app/auth/login", {"code": "dev:bob", "nickName": "鲍勃"})
check("bob login", r.get("code") == 200, r.get("msg", ""))
bob = r["data"]["token"]
bob_id = r["data"]["user"]["id"]

print("== 2. 发起审批 ==")
r = call("POST", "/app/order", {
    "title": "一把 1800 的机械键盘",
    "price": 1800,
    "reason": "现在用的键盘按下去有点粘连了，打字很难受，而且这款是静音的",
    "substitute": "家里还有一把薄膜键盘，但手感很差",
    "category": "digital",
    "visibility": "0",
    "images": ""
}, alice)
check("create order", r.get("code") == 200, r.get("msg", ""))
order_id = r.get("data")

print("== 3. 广场列表 ==")
r = call("GET", "/app/order/square?sort=new&pageNum=1&pageSize=10", None, bob)
check("square list", r.get("code") == 200, f"total={r.get('data', {}).get('total')}")
if r.get("code") == 200 and r["data"]["rows"]:
    row = r["data"]["rows"][0]
    check("有发起人昵称", bool(row.get("userNickName")), row.get("userNickName"))
    check("有倒计时", (row.get("remainSeconds") or 0) > 0, str(row.get("remainSeconds")))
    check("图片列表已解析", isinstance(row.get("imageList"), list))

print("== 4. 详情 ==")
r = call("GET", f"/app/order/{order_id}", None, bob)
check("detail", r.get("code") == 200, r.get("msg", ""))
if r.get("code") == 200:
    check("myVoted=false", r["data"]["order"].get("myVoted") is False)
    check("mine=false (bob)", r["data"]["order"].get("mine") is False)

print("== 5. 批阅 ==")
r = call("POST", "/app/vote", {"orderId": order_id, "result": "2", "reason": "家里那把还能用，先别买"}, bob)
check("bob reject", r.get("code") == 200, r.get("msg", ""))

r = call("POST", "/app/vote", {"orderId": order_id, "result": "1", "reason": "打字是生产力，买"}, bob)
check("重复批阅被拦截", r.get("code") != 200, r.get("msg", ""))

r = call("POST", "/app/vote", {"orderId": order_id, "result": "1", "reason": "自己批自己"}, alice)
check("自己不能批自己", r.get("code") != 200, r.get("msg", ""))

print("== 6. 批条列表 ==")
r = call("GET", f"/app/vote/list?orderId={order_id}&sort=hot", None, alice)
check("vote list", r.get("code") == 200, f"count={len(r.get('data') or [])}")
if r.get("code") == 200 and r["data"]:
    vid = r["data"][0]["id"]
    r2 = call("POST", f"/app/vote/{vid}/like", None, alice)
    check("点赞", r2.get("code") == 200 and r2.get("data") is True, r2.get("msg", ""))

print("== 7. 评论 ==")
r = call("POST", "/app/comment", {"orderId": order_id, "content": "这把键盘确实不错，我有一把"}, bob)
check("comment", r.get("code") == 200, r.get("msg", ""))
r = call("GET", f"/app/comment/list?orderId={order_id}", None, alice)
check("comment list", r.get("code") == 200, f"count={len(r.get('data') or [])}")

print("== 8. 个人中心 ==")
r = call("GET", "/app/auth/profile", None, bob)
check("profile", r.get("code") == 200, r.get("msg", ""))
if r.get("code") == 200:
    check("voteTotal>=1", (r["data"].get("voteTotal") or 0) >= 1, str(r["data"].get("voteTotal")))

print("== 9. 通知 ==")
r = call("GET", "/app/notice/list?pageNum=1&pageSize=10", None, alice)
check("notice list", r.get("code") == 200, f"count={len(r.get('data', {}).get('rows') or [])}")
r = call("GET", "/app/notice/unread", None, alice)
check("unread", r.get("code") == 200, str(r.get("data")))

print("== 10. 审批群 ==")
# 每人最多 3 个群。先解散上一轮跑出来的，保证脚本可以反复执行。
r = call("GET", "/app/group/mine", None, alice)
for g in (r.get("data") or []):
    if g.get("isOwner"):
        call("POST", f"/app/group/{g['id']}/dissolve", None, alice)

r = call("POST", "/app/group", {"name": "寝室的账本", "notice": "超过 500 的一律驳回"}, alice)
check("create group", r.get("code") == 200, r.get("msg", ""))
gid = r.get("data")
r = call("GET", "/app/group/mine", None, alice)
check("my groups", r.get("code") == 200, f"count={len(r.get('data') or [])}")
code = (r.get("data") or [{}])[0].get("inviteCode") if r.get("data") else None
check("有邀请码", bool(code), str(code))
if code:
    r = call("POST", f"/app/group/join?inviteCode={code}", None, bob)
    check("bob 加入群", r.get("code") == 200, r.get("msg", ""))

print("== 11. 举报 ==")
r = call("POST", "/app/report", {"targetType": "1", "targetId": order_id, "reasonType": "other", "content": "测试举报"}, bob)
check("report", r.get("code") == 200, r.get("msg", ""))

print("== 12. 收藏 ==")
r = call("POST", f"/app/order/{order_id}/favorite", None, bob)
check("favorite on", r.get("code") == 200 and r.get("data") is True)
r = call("GET", "/app/order/mine?type=favorite&pageNum=1&pageSize=10", None, bob)
check("favorite list", r.get("code") == 200, f"total={r.get('data', {}).get('total')}")

print("== 13. 未登录拦截 ==")
r = call("GET", "/app/order/square?pageNum=1&pageSize=10")
check("无 token 被拦截", r.get("code") == 401, str(r.get("code")))

print()
print(f"RESULT: pass={ok} fail={fail}")
