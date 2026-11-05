"""造一批「长内容」演示数据，用来验证详情弹层能不能正常滚动。

用法：python scripts/_seed_demo.py

做三件事：
1. 固定一个可复现的开发账号 dev:qa-mine（页面里 localStorage 写 wda_dev_id=qa-mine 即可登录）；
2. 给它建 2 条申请：一条购买理由写到 500 字以上，一条正常长度；
3. 让爱丽丝/鲍勃/卡罗尔去批，每条批语也写长，凑满一屏以上。
"""

import json
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8080"


def call(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("App-Token", token)
    try:
        with urllib.request.urlopen(req, timeout=25) as r:
            return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())


def login(code, nick):
    r = call("POST", "/app/auth/login", {"code": code, "nickName": nick})
    assert r.get("code") == 200, r
    return r["data"]["token"], r["data"]["user"]["id"]


LONG_REASON = (
    "先说说为什么突然想买这个东西。我这间卧室朝北，冬天不开窗也不通风，"
    "前阵子开始每天早上起来喉咙发干、鼻子痒，一开始以为是感冒，连着两周都这样，"
    "后来把被子床单全洗了一遍、除螨仪也吸了两遍，还是没好转，才怀疑是空气问题。"
    "用检测仪测了一下，PM2.5 常态在 60 到 80 之间，关门睡觉的话二氧化碳能到 1800ppm，"
    "难怪每天早上起来脑袋发沉。"
    "看了下这台机器：CADR 颗粒物 500 立方米每小时，按我这间 14 平米、层高 2.7 米算，"
    "一小时能循环 9 次以上，够用了；噪音最低档 22 分贝，比我现在的落地扇安静得多；"
    "滤芯一年一换，一套 399，摊下来一天一块一。"
    "价格 2399，比同 CADR 的另外两个牌子便宜四五百，贵的那款多的是加湿功能，"
    "但我冬天本来就有加湿器，重复了。"
    "想买它的核心理由是睡眠，我最近三个月平均入睡时间 1 点 40，"
    "早上起来还是累，如果能靠改善空气质量把睡眠质量拉回来一点，这钱花得值。"
    "还有一层考虑是长期账：我算过，如果这一台能用五年，"
    "按每天开 8 小时算，电费一年大概 90 块（额定功率 38 瓦，睡眠档不到 10 瓦），"
    "加滤芯 399，全年持有成本不到 500，平均一天一块三，"
    "比我每月点两次外卖的钱还少。"
    "对比过的方案有三个：一是只买新风机，要打孔，租房不能改结构，直接排除；"
    "二是买便宜的 599 款，CADR 只有 180，我这个房间要循环一遍得四十分钟，效率太低；"
    "三是买带除甲醛功能的，贵八百，但我这房子装修已经三年了，"
    "甲醛早就挥发得差不多，为用不上的功能多付钱不合适。"
    "所以最后锁定这台，是「够用的性能 + 合理的持有成本」，不是因为它便宜或者好看。"
    "最后再说说我的担心：这类家电最怕的是「买了才发现噪音大，晚上不敢开」，"
    "所以我会先去实体店听一下实际声音，如果最低档超过 30 分贝，我就放弃这个型号。"
    "不买的理由我也想过：一是怕买回来新鲜三天就闲置，二是滤芯是长期支出，"
    "三是搬家的话体积有点大。第一条我认，所以我想先定个规矩，"
    "买回来一个月内如果日均开机低于 4 小时，我就挂二手出掉，不再自我安慰。"
)

SHORT_REASON = "通勤地铁上太吵，想买个耳塞挡一挡，89 块，能接受。"

LONG_VOTE_1 = (
    "投通过，但想把话说清楚。参数对得起价格，CADR 500 在这个价位属于上游，滤芯 399 一年也厚道。"
    "我更在意他说的症状到底是不是空气造成的 —— 早上喉咙干、脑袋沉，跟 CO2 高、湿度低都能对上。"
    "建议买了顺带记两周温湿度数据，真是湿度问题，加湿器更该开。"
    "他说的「一个月日均开不到 4 小时就出掉」这条自律条款，我认。"
)

LONG_VOTE_2 = (
    "驳回，三点理由。第一，2399 不是小钱，他上个月刚买 1800 的键盘、再往前 600 的咖啡机，"
    "这频率已经是用购物解决情绪了。第二，症状更像不通风加干燥，开窗二十分钟加几十块的加湿器，"
    "成本不到净化器零头。第三，他自己先说「怕闲置」，说明心里也没底。"
    "先试两周低成本方案，不行再来提，我第一个投通过。"
)

LONG_VOTE_3 = (
    "投通过，补充一点别人没提到的：他那间卧室朝北，冬天基本不开窗，"
    "这种环境里净化器的价值不只是过滤颗粒物，更在于强制空气流动，"
    "配合定时开一档，能明显改善闷的感觉。我家里同款用了两年，"
    "最直观的变化是灰尘少了，桌面一周不擦也不明显。"
    "至于滤芯成本，一年 399 摊到每天一块一，比一杯奶茶便宜，不算负担。"
    "唯一提醒：买之前量一下床边到插座的距离，这机器线长只有 1.8 米，"
    "放太远要接插排。"
)

SHORT_VOTE_4 = "同意楼上，先花小钱验证问题，别一上来就上设备。"
SHORT_VOTE_5 = "我站通过，主要是他连「一个月闲置就挂二手」这种话都写出来了，不像冲动消费。"


def main() -> None:
    mine, mine_id = login("dev:qa-mine", "测试小李")
    alice, _ = login("dev:alice", "爱丽丝")
    bob, _ = login("dev:bob", "鲍勃")
    carol, _ = login("dev:carol", "卡罗尔")
    dave, _ = login("dev:dave", "戴夫")

    # 清掉这个账号上一轮造的单，避免重复堆积（逻辑删除，不占当日额度）
    r = call("GET", "/app/order/mine?type=apply&pageNum=1&pageSize=50", None, mine)
    for row in (r.get("data", {}).get("rows") or []):
        call("DELETE", f"/app/order/{row['id']}", None, mine)

    # 顺手把头像设成预设头像，截图好看点
    call("PUT", "/app/auth/profile?nickName=%E6%B5%8B%E8%AF%95%E5%B0%8F%E6%9D%8E&avatar=preset%3Aav02", None, mine)

    print("① 长理由 + 多人审批")
    r = call("POST", "/app/order", {
        "title": "一台 2399 的空气净化器",
        "price": 2399,
        "reason": LONG_REASON,
        "substitute": "先花 60 块买个温湿度计 + 每天开窗通风，观察两周",
        "category": "home",
        "visibility": "0",
        "approveMode": "1",
        "durationHours": 48,
        "images": "",
    }, mine)
    assert r.get("code") == 200, r
    long_id = r["data"]
    print("   单号", long_id, f"理由 {len(LONG_REASON)} 字")

    print("② 短理由 + 多人审批")
    r = call("POST", "/app/order", {
        "title": "一副 89 的降噪耳塞",
        "price": 89,
        "reason": SHORT_REASON,
        "substitute": "用手机自带的耳机凑合",
        "category": "digital",
        "visibility": "0",
        "approveMode": "1",
        "durationHours": 24,
        "images": "",
    }, mine)
    assert r.get("code") == 200, r
    short_id = r["data"]
    print("   单号", short_id)

    print("③ 四个人给长理由那单写批语")
    voters = [
        (alice, LONG_VOTE_1, "1"),
        (bob, LONG_VOTE_2, "2"),
        (carol, LONG_VOTE_3, "1"),
        (dave, SHORT_VOTE_4, "2"),
    ]
    for tk, reason, result in voters:
        r = call("POST", "/app/vote", {"orderId": long_id, "result": result, "reason": reason}, tk)
        print("   ", "通过" if result == "1" else "驳回", "→", r.get("code"), r.get("msg", ""), f"{len(reason)} 字")

    print("④ 再说两句闲话（评论区）")
    for tk, text in ((alice, SHORT_VOTE_5), (bob, "楼上说得对，但我觉得他执行力还行，先买也不是不能接受。")):
        r = call("POST", "/app/comment", {"orderId": long_id, "content": text}, tk)
        print("   评论 →", r.get("code"), r.get("msg", ""), f"{len(text)} 字")

    # 短单也投一票，让列表上有票数
    call("POST", "/app/vote", {"orderId": short_id, "result": "1", "reason": "89 块，批了"}, alice)

    # 状态回读
    for name, oid in (("长理由", long_id), ("短理由", short_id)):
        r = call("GET", f"/app/order/{oid}", None, mine)
        o = (r.get("data") or {}).get("order") or {}
        votes = (r.get("data") or {}).get("votes") or []
        print(f"   回读 {name}: status={o.get('status')} 票={o.get('passCount')}/{o.get('rejectCount')} 批条={len(votes)}")

    print("\n登录方式：页面 localStorage 写 wda_dev_id = 'qa-mine'（清掉 wda_token / wda_user 后刷新）")
    print(f"LONG_ID={long_id}")
    print(f"SHORT_ID={short_id}")


if __name__ == "__main__":
    main()
