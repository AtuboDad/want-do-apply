"""钱包请等等 - 管理端接口冒烟测试

管理端登录接口带 @ApiEncrypt，请求体必须用 AES 加密，
AES 秘钥再用 RSA 公钥加密后放进 encrypt-key 头，这里按同样协议实现。

运行前提：后端已在 127.0.0.1:8080 启动。
"""
import base64
import json
import os
import re
import secrets
import string
import urllib.error
import urllib.parse
import urllib.request

from cryptography.hazmat.primitives import padding, serialization
from cryptography.hazmat.primitives.asymmetric import padding as asym_padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

BASE = "http://127.0.0.1:8080"
CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e"
YML = os.path.join(os.path.dirname(__file__), "..", "backend", "ruoyi-admin", "src", "main", "resources", "application.yml")

ok = 0
fail = 0

# Authorization 头是否用裸 token（不带 Bearer 前缀），登录后自动探测
AUTH_RAW = {"v": False}


def load_rsa_public_key():
    """从 application.yml 的 api-decrypt.privateKey 推导出公钥"""
    with open(YML, encoding="utf-8") as f:
        text = f.read()
    # api-decrypt 段里的 privateKey 是最长的那串 base64
    keys = re.findall(r"^\s*privateKey:\s*(\S+)\s*$", text, re.M)
    keys = [k for k in keys if len(k) > 200]
    if not keys:
        raise RuntimeError("application.yml 里没找到 api-decrypt.privateKey")
    priv = serialization.load_der_private_key(base64.b64decode(keys[0]), password=None)
    return priv.public_key()


PUB = load_rsa_public_key()


def encrypt_body(obj):
    """按 RuoYi 的协议加密请求体，返回 (密文, encrypt-key 头)"""
    aes_key = "".join(secrets.choice(string.ascii_letters + string.digits) for _ in range(16))
    raw = json.dumps(obj, ensure_ascii=False).encode("utf-8")
    padder = padding.PKCS7(128).padder()
    padded = padder.update(raw) + padder.finalize()
    enc = Cipher(algorithms.AES(aes_key.encode()), modes.ECB()).encryptor()
    body = base64.b64encode(enc.update(padded) + enc.finalize()).decode()
    # 头部 = RSA(Base64(aes_key))
    key_b64 = base64.b64encode(aes_key.encode())
    header = base64.b64encode(PUB.encrypt(key_b64, asym_padding.PKCS1v15())).decode()
    return body, header


def call(method, path, body=None, token=None, encrypt=False):
    url = BASE + path
    headers = {"Content-Type": "application/json"}
    if token:
        # Sa-Token 的 token-prefix 未配置时不带 Bearer 前缀，登录后自动探测一次
        headers["Authorization"] = token if AUTH_RAW["v"] else "Bearer " + token
        # RuoYi 会校验 header 里的 clientid 与 token 中的是否一致
        headers["clientid"] = CLIENT_ID
    if body is not None and encrypt:
        text, header = encrypt_body(body)
        data = text.encode()
        headers["encrypt-key"] = header
    else:
        data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return json.loads(raw)
        except Exception:
            return {"code": e.code, "msg": raw[:200]}


def q(s):
    """URL 查询参数编码（中文必须转义）"""
    return urllib.parse.quote(str(s), safe="")


def check(name, cond, extra=""):
    global ok, fail
    if cond:
        ok += 1
        print(f"  [PASS] {name} {extra}")
    else:
        fail += 1
        print(f"  [FAIL] {name} {extra}")


print("== 1. 管理端登录（加密请求体）==")
r = call("POST", "/auth/login", {
    "clientId": CLIENT_ID,
    "grantType": "password",
    "username": "admin",
    "password": "admin123"
}, encrypt=True)
check("admin login", r.get("code") == 200, r.get("msg", ""))
token = (r.get("data") or {}).get("access_token")
check("拿到 access_token", bool(token), (token or "")[:16] + "...")
if not token:
    print("\n登录失败，后续跳过")
    raise SystemExit(1)

# 探测 Authorization 头格式：带不带 Bearer 前缀取决于 sa-token.token-prefix
AUTH_RAW["v"] = False
probe = call("GET", "/apply/dashboard/overview", None, token)
if probe.get("code") != 200:
    AUTH_RAW["v"] = True
    probe2 = call("GET", "/apply/dashboard/overview", None, token)
    check("鉴权头格式探测", probe2.get("code") == 200, "裸 token" if probe2.get("code") == 200 else probe2.get("msg", ""))
else:
    check("鉴权头格式探测", True, "Bearer 前缀")

print("== 2. 数据看板 ==")
r = call("GET", "/apply/dashboard/overview", None, token)
check("dashboard", r.get("code") == 200, r.get("msg", ""))
if r.get("code") == 200:
    d = r["data"]
    for k in ("userTotal", "orderTotal", "voteTotal", "reportPending"):
        check(f"看板有 {k}", k in d, str(d.get(k)))
    check("趋势为数组", isinstance(d.get("trend"), list), f"len={len(d.get('trend') or [])}")
    check("品类分布为数组", isinstance(d.get("categories"), list), f"len={len(d.get('categories') or [])}")

print("== 3. 六个列表接口 ==")
for name, path in [
    ("审批单", "/apply/order/list?pageNum=1&pageSize=5"),
    ("批条", "/apply/vote/list?pageNum=1&pageSize=5"),
    ("评论", "/apply/comment/list?pageNum=1&pageSize=5"),
    ("审批群", "/apply/group/list?pageNum=1&pageSize=5"),
    ("小程序用户", "/apply/user/list?pageNum=1&pageSize=5"),
    ("举报", "/apply/report/list?pageNum=1&pageSize=5"),
]:
    r = call("GET", path, None, token)
    total = (r.get("data") or {}).get("total")
    check(f"{name}列表", r.get("code") == 200, f"total={total}")

r = call("GET", "/apply/report/pending", None, token)
check("待处理举报数", r.get("code") == 200, str(r.get("data")))

print("== 4. 造一张待删的审批单 ==")
r = call("POST", "/app/auth/login", {"code": "dev:smoke_admin", "nickName": "冒烟测试员"})
app_token = (r.get("data") or {}).get("token")
app_uid = (r.get("data") or {}).get("user", {}).get("id")
check("冒烟用户登录", r.get("code") == 200, r.get("msg", ""))

r = call("POST", "/app/order", {
    "title": "冒烟测试商品",
    "price": 999,
    "reason": "这条记录由 _smoke_admin.py 创建，跑完会删掉",
    "category": "other",
    "visibility": "0"
}, app_token)
check("创建测试单", r.get("code") == 200, r.get("msg", ""))
oid = r.get("data")

r = call("GET", f"/apply/order/list?id={oid}&pageNum=1&pageSize=5", None, token)
check("按 ID 精确查询", (r.get("data") or {}).get("total") == 1, f"total={(r.get('data') or {}).get('total')}")

print("== 5. 置顶 / 取消置顶 ==")
r = call("PUT", f"/apply/order/{oid}/top?isTop=1", None, token)
check("置顶", r.get("code") == 200, r.get("msg", ""))
r = call("GET", f"/apply/order/list?id={oid}", None, token)
row = ((r.get("data") or {}).get("rows") or [{}])[0]
check("置顶已生效", row.get("isTop") == "1", str(row.get("isTop")))
r = call("PUT", f"/apply/order/{oid}/top?isTop=0", None, token)
check("取消置顶", r.get("code") == 200, r.get("msg", ""))

print("== 6. 下架 / 恢复 ==")
r = call("PUT", f"/apply/order/{oid}/audit?auditStatus=2&remark=" + q("冒烟下架"), None, token)
check("下架", r.get("code") == 200, r.get("msg", ""))
r = call("GET", f"/apply/order/list?id={oid}", None, token)
row = ((r.get("data") or {}).get("rows") or [{}])[0]
check("下架已生效", row.get("auditStatus") == "2", str(row.get("auditStatus")))
r = call("PUT", f"/apply/order/{oid}/audit?auditStatus=1&remark=" + q("冒烟恢复"), None, token)
check("恢复", r.get("code") == 200, r.get("msg", ""))

print("== 7. 举报 -> 处理 -> 同步下架 ==")
r = call("POST", "/app/report", {"targetType": "1", "targetId": oid, "reasonType": "spam", "content": "冒烟举报"}, app_token)
check("提交举报", r.get("code") == 200, r.get("msg", ""))

r = call("GET", "/apply/report/list?status=0&pageNum=1&pageSize=50", None, token)
reports = (r.get("data") or {}).get("rows") or []
mine = [x for x in reports if x.get("targetId") == oid and x.get("targetType") == "1"]
check("举报出现在待处理列表", len(mine) == 1, f"matched={len(mine)}")
if mine:
    rep = mine[0]
    check("举报带出对象摘要", bool(rep.get("targetSummary")), str(rep.get("targetSummary"))[:20])
    r = call("PUT", "/apply/report/handle?takeDown=true", {
        "id": rep["id"],
        "targetType": rep["targetType"],
        "targetId": rep["targetId"],
        "status": "1",
        "handleRemark": "冒烟：内容违规已下架"
    }, token)
    check("处理举报", r.get("code") == 200, r.get("msg", ""))
    r = call("GET", f"/apply/order/list?id={oid}", None, token)
    row = ((r.get("data") or {}).get("rows") or [{}])[0]
    check("被举报单已下架", row.get("auditStatus") == "2", str(row.get("auditStatus")))

print("== 8. 编辑小程序用户 ==")
r = call("PUT", "/apply/user", {
    "id": app_uid,
    "status": "0",
    "creditScore": 88,
    "nickName": "冒烟测试员"
}, token)
check("编辑用户", r.get("code") == 200, r.get("msg", ""))
r = call("GET", "/apply/user/list?keyword=" + q("冒烟") + "&pageNum=1&pageSize=10", None, token)
rows = (r.get("data") or {}).get("rows") or []
hit = [x for x in rows if x.get("id") == app_uid]
check("用户可被搜到", len(hit) == 1, f"matched={len(hit)}")
if hit:
    check("信用分已更新", hit[0].get("creditScore") == 88, str(hit[0].get("creditScore")))

print("== 9. 删除审批单 ==")
r = call("DELETE", f"/apply/order/{oid}", None, token)
check("删除", r.get("code") == 200, r.get("msg", ""))
r = call("GET", f"/apply/order/list?id={oid}", None, token)
check("删除后查不到", (r.get("data") or {}).get("total") == 0, f"total={(r.get('data') or {}).get('total')}")

print("== 10. 未登录拦截 ==")
r = call("GET", "/apply/order/list?pageNum=1&pageSize=5")
check("无 token 被拦截", r.get("code") == 401, str(r.get("code")))

print()
print(f"RESULT: pass={ok} fail={fail}")
