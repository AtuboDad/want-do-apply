# -*- coding: utf-8 -*-
"""
「我的」页搜索回归（可复跑）。

覆盖两件事：
  1) 正常搜索 / 清空 / 关键词+状态组合 是否都对
  2) 首屏列表还没回来就去搜索，请求会不会被 loading 吞掉（曾经的 bug）

自备数据：用一个固定的 dev 账号，每次跑前清空它名下的单再新建一条。

用法（需要 H5 dev server 跑在 5175、后端在 8080）：
    python scripts/_search_test.py
"""
import json
import re
import time
import urllib.request
from urllib.parse import quote

from playwright.sync_api import sync_playwright

API = "http://127.0.0.1:8080"
BASE = "http://localhost:5175"
DEV = "searchprobe1"
TITLE = "搜索验证 一把 88 的小台灯"


def call(method, path, body=None, token=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(API + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("App-Token", token)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))


def norm(u):
    return re.sub(r"^.*?/app/", "/app/", u)


JS_CARDS = "() => [...document.querySelectorAll('.order-card')].map(e=>e.innerText.replace(/\\n+/g,' | '))"
JS_EMPTY = "() => document.querySelector('.empty') ? document.querySelector('.empty').innerText.replace(/\\n+/g,' | ') : ''"


def seed():
    """确保这个 dev 账号名下只有一条固定单据"""
    tok = call("POST", "/app/auth/login", {"code": "dev:" + DEV, "nickName": "搜索探针"})["data"]["token"]
    old = call("GET", "/app/order/mine?type=apply&pageNum=1&pageSize=50", None, tok)
    for row in (old.get("data", {}).get("rows") or []):
        call("DELETE", f"/app/order/{row['id']}", None, tok)
    r = call("POST", "/app/order", {
        "title": TITLE, "price": 88,
        "reason": "书桌上缺个灯，晚上看书眼睛累",
        "substitute": "", "category": "home", "visibility": "0",
        "images": "", "approveMode": "1", "durationHours": 2
    }, tok)
    assert r.get("code") == 200, r
    return tok


def enter_mine(pg):
    pg.goto(BASE + "/#/pages/index/index", wait_until="networkidle")
    pg.wait_for_timeout(1000)
    pg.evaluate("() => { localStorage.clear(); localStorage.setItem('wda_dev_id', '%s'); }" % DEV)
    pg.reload(wait_until="networkidle")
    pg.wait_for_timeout(2000)
    pg.evaluate("() => { location.hash = '#/pages/mine/index' }")
    pg.wait_for_selector(".search input", timeout=8000)


def main():
    seed()

    with sync_playwright() as p:
        b = p.chromium.launch(headless=True, channel="chrome")
        ctx = b.new_context(viewport={"width": 390, "height": 844}, device_scale_factor=2)
        pg = ctx.new_page()
        reqs = []
        pg.on("request", lambda r: reqs.append(r.url) if "/order/mine" in r.url else None)

        # ---------- 1. 正常搜索 ----------
        print("=== 1. 正常搜索（无网络延迟） ===")
        enter_mine(pg)
        pg.wait_for_timeout(2500)
        print("  初始列表条数:", len(pg.evaluate(JS_CARDS)))

        reqs.clear()
        pg.evaluate("() => document.querySelector('.search input').focus()")
        pg.keyboard.type("台灯")
        pg.wait_for_timeout(2000)
        print("  搜「台灯」请求:", [norm(u) for u in reqs])
        print("  搜「台灯」条数:", len(pg.evaluate(JS_CARDS)), "→", "✅" if len(pg.evaluate(JS_CARDS)) == 1 else "❌")
        pg.screenshot(path=r"E:\workspaces\WorkBuddy\want-do-apply\.playwright-cli\final\10-search-mine.png")

        reqs.clear()
        pg.keyboard.press("Control+A")
        pg.keyboard.press("Backspace")
        pg.wait_for_timeout(3000)
        print("  清空后条数:", len(pg.evaluate(JS_CARDS)), "→", "✅" if len(pg.evaluate(JS_CARDS)) == 1 else "❌")

        pg.evaluate("() => document.querySelector('.search input').focus()")
        pg.keyboard.type("不存在的词zzz")
        pg.wait_for_timeout(2200)
        print("  搜不存在:", len(pg.evaluate(JS_CARDS)), "条 |", pg.evaluate(JS_EMPTY))

        # ---------- 2. 首屏未回来就搜索 ----------
        print("\n=== 2. 首屏请求还在路上就搜索（回归项） ===")
        # 先把搜索框清干净并等状态落定，否则这页是 keep-alive 的，
        # 上一段的关键词会残留下来，输入被拼成「不存在的词zzz台灯」
        pg.evaluate("() => document.querySelector('.search input').focus()")
        pg.keyboard.press("Control+A")
        pg.keyboard.press("Backspace")
        pg.wait_for_timeout(2500)

        pg.evaluate("() => { location.hash = '#/pages/index/index' }")
        pg.wait_for_timeout(1200)

        cdp = ctx.new_cdp_session(pg)
        cdp.send("Network.enable")
        cdp.send("Network.emulateNetworkConditions",
                 {"offline": False, "latency": 3000, "downloadThroughput": -1, "uploadThroughput": -1})

        reqs.clear()
        pg.evaluate("() => { location.hash = '#/pages/mine/index' }")
        pg.wait_for_selector(".search input", timeout=8000)
        pg.evaluate("() => document.querySelector('.search input').focus()")
        pg.keyboard.type("台灯", delay=30)
        pg.wait_for_timeout(12000)

        kw = [norm(u) for u in reqs if "keyword=" + quote("台灯") in u]
        print("  请求序列:")
        for u in reqs:
            print("   ", norm(u))
        print("  带关键词的请求数:", len(kw), "→", "✅ 没被吞" if kw else "❌ 被 loading 吞了")
        print("  最终列表条数:", len(pg.evaluate(JS_CARDS)))
        cdp.send("Network.emulateNetworkConditions",
                 {"offline": False, "latency": 0, "downloadThroughput": -1, "uploadThroughput": -1})
        b.close()


if __name__ == "__main__":
    main()
