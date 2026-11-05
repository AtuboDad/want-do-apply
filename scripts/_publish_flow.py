#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""验证「发一个 → 发到广场 → 自动落到我提交的 → 列表里能看见」这条闭环。"""
import json
import time

from playwright.sync_api import sync_playwright

BASE = "http://localhost:5175"
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
TITLE = "发布闭环验证-" + str(int(time.time()))[-6:]


def main():
    with sync_playwright() as p:
        b = p.chromium.launch(headless=True, executable_path=CHROME)
        ctx = b.new_context(viewport={"width": 390, "height": 844}, device_scale_factor=2)
        pg = ctx.new_page()

        pg.goto(BASE + "/#/pages/index/index", wait_until="networkidle")
        pg.wait_for_timeout(2600)

        print("① 点「发一个」，计时")
        t0 = time.time()
        pg.evaluate(
            """() => document.querySelectorAll('uni-tabbar .uni-tabbar__item')[1]
                    .dispatchEvent(new MouseEvent('click', { bubbles: true }))"""
        )
        pg.wait_for_selector(".hero-title", timeout=8000)
        print(f"   首屏可见 {(time.time()-t0)*1000:.0f} ms → hash={pg.evaluate('() => location.hash')}")

        print("\n② 填表")
        pg.evaluate(
            """(title) => {
              const set = (el, v) => {
                const d = Object.getOwnPropertyDescriptor(el.__proto__, 'value');
                d.set.call(el, v);
                el.dispatchEvent(new Event('input', { bubbles: true }));
              };
              const inputs = [...document.querySelectorAll('.form input')];
              set(inputs[0], title);
              set(inputs[1], '268');
            }""",
            TITLE,
        )
        pg.wait_for_timeout(300)
        pg.evaluate(
            """() => {
              const ta = document.querySelector('.form textarea');
              const d = Object.getOwnPropertyDescriptor(ta.__proto__, 'value');
              d.set.call(ta, '这条是闭环验证用的，发出去之后应该自动跳到「我提交的」，并且列表第一条就是它。');
              ta.dispatchEvent(new Event('input', { bubbles: true }));
            }"""
        )
        pg.wait_for_timeout(400)

        print("\n③ 滚到底点「发到广场」")
        pg.mouse.move(195, 500)
        for _ in range(6):
            pg.mouse.wheel(0, 600)
            pg.wait_for_timeout(120)
        pg.wait_for_timeout(500)
        r = pg.evaluate(
            """() => {
              const bt = [...document.querySelectorAll('uni-view, uni-button, button')]
                .find(e => (e.innerText || '').trim() === '发到广场');
              if (!bt) return null;
              const b = bt.getBoundingClientRect();
              return { x: Math.round(b.x + b.width/2), y: Math.round(b.y + b.height/2) };
            }"""
        )
        print("   按钮:", r)
        reqs = []
        pg.on("request", lambda q: reqs.append(q.url) if "/order" in q.url else None)
        t1 = time.time()
        pg.mouse.click(r["x"], r["y"])

        # 等它自己跳走
        jumped = None
        for _ in range(20):
            pg.wait_for_timeout(250)
            h = pg.evaluate("() => location.hash")
            if "mine" in h:
                jumped = (time.time() - t1) * 1000
                break
        print(f"   自动跳到「我的」: {jumped:.0f} ms" if jumped else "   ❌ 没有自动跳")
        print("   建单请求:", [u.split('/app')[-1][:40] for u in reqs])

        pg.wait_for_timeout(3000)
        res = pg.evaluate(
            """(title) => {
              const txt = document.body.innerText;
              const cards = [...document.querySelectorAll('.order-card')];
              const first = cards[0];
              return {
                hash: location.hash,
                hasTitle: txt.includes(title),
                firstTitle: (first?.querySelector('.title')?.innerText || '').trim(),
                count: cards.length,
                statApply: (document.querySelector('.stat .num')?.innerText || '').trim()
              };
            }""",
            TITLE,
        )
        print("   ", json.dumps(res, ensure_ascii=False))
        ok = res["hasTitle"] and res["hash"].endswith("/pages/mine/index")
        print("   闭环:", "✅ 发完就在列表第一条" if ok else "❌")
        pg.screenshot(path=".playwright-cli/final/46-publish-to-mine.png")

        b.close()


if __name__ == "__main__":
    main()
