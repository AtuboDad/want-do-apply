"""钱包请等等 - 后端接口冒烟测试"""
import json
import urllib.request
import urllib.error
from urllib.parse import quote

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
# 每人每天最多发 10 单，重复跑会撞限额：先把上一轮自己提交的单逻辑删除掉（不计入当日额度）
for tk in (alice, bob):
    r = call("GET", "/app/order/mine?type=apply&pageNum=1&pageSize=50", None, tk)
    for row in (r.get("data", {}).get("rows") or []):
        call("DELETE", f"/app/order/{row['id']}", None, tk)

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

print("== 9. 头像设置 ==")
# 预设头像存 preset:av05 这种标记，前端自己换成包内图片
r = call("PUT", "/app/auth/profile?avatar=preset%3Aav05", None, bob)
check("设置预设头像", r.get("code") == 200, r.get("msg", ""))
r = call("GET", "/app/auth/me", None, bob)
check("头像已保存", (r.get("data") or {}).get("avatar") == "preset:av05", str((r.get("data") or {}).get("avatar")))
# 任意字符串不许写库
r = call("PUT", "/app/auth/profile?avatar=javascript%3Aalert(1)", None, bob)
check("非法头像被拒", r.get("code") != 200, r.get("msg", ""))
r = call("GET", "/app/auth/me", None, bob)
check("被拒后没写库", (r.get("data") or {}).get("avatar") == "preset:av05", str((r.get("data") or {}).get("avatar")))

print("== 10. 通知 ==")
r = call("GET", "/app/notice/list?pageNum=1&pageSize=10", None, alice)
check("notice list", r.get("code") == 200, f"count={len(r.get('data', {}).get('rows') or [])}")
r = call("GET", "/app/notice/unread", None, alice)
check("unread", r.get("code") == 200, str(r.get("data")))

print("== 11. 审批群 ==")
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

print("== 12. 举报 ==")
r = call("POST", "/app/report", {"targetType": "1", "targetId": order_id, "reasonType": "other", "content": "测试举报"}, bob)
check("report", r.get("code") == 200, r.get("msg", ""))

print("== 13. 收藏 ==")
r = call("POST", f"/app/order/{order_id}/favorite", None, bob)
check("favorite on", r.get("code") == 200 and r.get("data") is True)
r = call("GET", "/app/order/mine?type=favorite&pageNum=1&pageSize=10", None, bob)
check("favorite list", r.get("code") == 200, f"total={r.get('data', {}).get('total')}")

print("== 14. 未登录拦截 ==")
r = call("GET", "/app/order/square?pageNum=1&pageSize=10")
check("无 token 被拦截", r.get("code") == 401, str(r.get("code")))

print("== 15. 审批时长 / 补充说明 / 提前结束 ==")
# 新建一单，指定 2 小时（审批时长由发起人自由设置）
r = call("POST", "/app/order", {
    "title": "一台 999 的咖啡机",
    "price": 999,
    "reason": "每天早上都要买一杯，一个月三百多，自己做更划算",
    "category": "home",
    "visibility": "0",
    "durationHours": 2,
    "images": ""
}, alice)
check("create with duration", r.get("code") == 200, r.get("msg", ""))
o2 = r.get("data")

r = call("GET", f"/app/order/{o2}", None, alice)
check("durationHours=2", r["data"]["order"].get("durationHours") == 2, str(r["data"]["order"].get("durationHours")))
rem = r["data"]["order"].get("remainSeconds") or 0
check("2 小时倒计时", 7000 < rem <= 7200, str(rem))

r = call("POST", "/app/order", {
    "title": "时长越界测试", "price": 10, "reason": "就是想试试看会怎样",
    "visibility": "0", "durationHours": 999
}, alice)
check("超长审批时长被拦截", r.get("code") != 200, r.get("msg", ""))

r = call("POST", f"/app/order/{o2}/note", {"note": "旧咖啡机已经漏水了"}, alice)
check("补充说明", r.get("code") == 200, r.get("msg", ""))
r = call("GET", f"/app/order/{o2}", None, alice)
check("补充说明已保存", r["data"]["order"].get("extraNote") == "旧咖啡机已经漏水了",
      str(r["data"]["order"].get("extraNote")))

r = call("POST", f"/app/order/{o2}/note", {"note": "我不是发起人"}, bob)
check("非发起人不能补充说明", r.get("code") != 200, r.get("msg", ""))

# 0 票时不让提前结束，避免误操作把单子弄成流拍
r = call("POST", f"/app/order/{o2}/finish", None, alice)
check("0 票提前结束被拦截", r.get("code") != 200, r.get("msg", ""))

call("POST", "/app/vote", {"orderId": o2, "result": "2", "reason": "外卖咖啡更香"}, bob)
r = call("POST", f"/app/order/{o2}/finish", None, bob)
check("非发起人不能提前结束", r.get("code") != 200, r.get("msg", ""))

r = call("POST", f"/app/order/{o2}/finish", None, alice)
check("提前结束", r.get("code") == 200, r.get("msg", ""))
check("提前结束结果=驳回", r.get("data") == "2", str(r.get("data")))
r = call("GET", f"/app/order/{o2}", None, alice)
check("状态已结案", r["data"]["order"].get("status") == "2", str(r["data"]["order"].get("status")))
check("结案后写不了补充说明",
      call("POST", f"/app/order/{o2}/note", {"note": "事后补充"}, alice).get("code") != 200)

# 早期「攒够 10 条批条自动结案」的规则已废弃：只有 1 票的单子仍处于审批中
r = call("GET", f"/app/order/{order_id}", None, alice)
check("票数少也不提前结案", r["data"]["order"].get("status") == "0", str(r["data"]["order"].get("status")))


print("== 16. 审批方式 / 理由选填 / 我的筛选 ==")
# 单人审批：一人表态就出结果
r = call("POST", "/app/order", {
    "title": "单人审批的一本书",
    "price": 59,
    "reason": "想看点轻松的东西，通勤路上翻翻",
    "visibility": "0",
    "approveMode": "0",
    "durationHours": 6,
    "images": ""
}, alice)
check("create single-mode", r.get("code") == 200, r.get("msg", ""))
o3 = r.get("data")
r = call("GET", f"/app/order/{o3}", None, alice)
check("approveMode=0", r["data"]["order"].get("approveMode") == "0", str(r["data"]["order"].get("approveMode")))

# 理由不填也能批
r = call("POST", "/app/vote", {"orderId": o3, "result": "1", "reason": ""}, bob)
check("不写理由也能批", r.get("code") == 200, r.get("msg", ""))
r = call("GET", f"/app/order/{o3}", None, alice)
check("单人审批一人表态即通过", r["data"]["order"].get("status") == "1",
      str(r["data"]["order"].get("status")))
check("无理由的批条也能返回", (r["data"].get("votes") or [{}])[0].get("reason") == "",
      repr((r["data"].get("votes") or [{}])[0].get("reason")))

# 审批方式传非法值要拦住
r = call("POST", "/app/order", {
    "title": "审批方式非法", "price": 10, "reason": "就是想试试看会怎样",
    "visibility": "0", "approveMode": "9", "durationHours": 1
}, alice)
check("非法审批方式被拦截", r.get("code") != 200, r.get("msg", ""))

# 我的：搜索
r = call("GET", "/app/order/mine?type=apply&keyword=" + quote("咖啡机") + "&pageNum=1&pageSize=20", None, alice)
rows = (r.get("data") or {}).get("rows") or []
check("搜索命中标题", r.get("code") == 200 and len(rows) >= 1, f"count={len(rows)}")
check("搜索结果都含关键字", all("咖啡机" in (x.get("title") or "") for x in rows) if rows else False)

r = call("GET", "/app/order/mine?type=apply&keyword=" + quote("不存在的关键词xyz") + "&pageNum=1&pageSize=20", None, alice)
check("搜不到时返回空", ((r.get("data") or {}).get("total") or 0) == 0,
      str((r.get("data") or {}).get("total")))

# 我的：状态过滤
r = call("GET", "/app/order/mine?type=apply&status=2&pageNum=1&pageSize=20", None, alice)
rows = (r.get("data") or {}).get("rows") or []
check("状态筛选只返回已驳回", all(x.get("status") == "2" for x in rows) if rows else True,
      f"count={len(rows)}")

r = call("GET", "/app/order/mine?type=apply&pageNum=1&pageSize=20", None, alice)
all_total = (r.get("data") or {}).get("total") or 0
r2 = call("GET", "/app/order/mine?type=apply&status=2&pageNum=1&pageSize=20", None, alice)
rej_total = (r2.get("data") or {}).get("total") or 0
check("筛选后总数不大于全部", rej_total <= all_total, f"{rej_total}<={all_total}")

# 新用户默认头像
r = call("POST", "/app/auth/login", {"code": "dev:avatarcheck", "nickName": "头像检查员"})
check("新用户带默认头像",
      str((r.get("data") or {}).get("user", {}).get("avatar", "")).startswith("preset:av"),
      str((r.get("data") or {}).get("user", {}).get("avatar")))

print()
print(f"RESULT: pass={ok} fail={fail}")
