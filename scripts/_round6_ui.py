#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""UI 验证（第六轮）：围观按钮从列表挪到详情 + 「我的」空态账号自查。"""
import json

from playwright.sync_api import sync_playwright

BASE = "http://localhost:5175"
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
SHOT = ".playwright-cli/final"


def shot(pg, name):
    pg.screenshot(path=f"{SHOT}/{name}")
    print("   截图:", name)


CENTER_JS = """([sel, nth]) => {
  const el = document.querySelectorAll(sel)[nth];
  if (!el) return null;
  const b = el.getBoundingClientRect();
  return { x: Math.round(b.x + b.width / 2), y: Math.round(b.y + b.height / 2) };
}"""


def click_xy(pg, sel, nth=0):
    r = pg.evaluate(CENTER_JS, [sel, nth])
    if not r:
        print("   ⚠️ 找不到可点元素:", sel, nth)
        return False
    pg.mouse.click(r["x"], r["y"])
    return True


def main():
    with sync_playwright() as p:
        b = p.chromium.launch(headless=True, executable_path=CHROME)
        ctx = b.new_context(viewport={"width": 390, "height": 844}, device_scale_factor=2)
        pg = ctx.new_page()

        print("① 广场：卡片上不该再有围观按钮")
        pg.goto(BASE + "/#/pages/index/index", wait_until="networkidle")
        pg.wait_for_timeout(2600)
        info = pg.evaluate(
            """() => ({
                 cards: document.querySelectorAll('.order-card').length,
                 favInCard: document.querySelectorAll('.order-card .fav').length,
                 favText: [...document.querySelectorAll('.order-card')]
                   .filter(c => c.innerText.includes('围观')).length
               })"""
        )
        print("   ", json.dumps(info, ensure_ascii=False))
        print("   卡片内围观按钮:", "✅ 已移除" if info["favInCard"] == 0 else "❌ 还在")
        shot(pg, "60-square-no-fav.png")

        print("\n② 打开详情：底部应有围观按钮")
        opened = click_xy(pg, ".order-card")
        pg.wait_for_timeout(2200)
        det = pg.evaluate(
            """() => {
              const btn = document.querySelector('.fav-btn');
              const bar = document.querySelector('.action-bar');
              return {
                hasBtn: !!btn,
                label: btn ? btn.innerText.replace(/\\n/g, ' ').trim() : null,
                on: btn ? btn.classList.contains('on') : null,
                barText: bar ? bar.innerText.replace(/\\n/g, ' ').trim() : null
              };
            }"""
        )
        print("   ", json.dumps(det, ensure_ascii=False))
        print("   详情内围观:", "✅ 有" if det["hasBtn"] else "❌ 没有")
        shot(pg, "61-detail-fav.png")

        print("\n③ 点一下围观 → 应变「已围观」")
        if det["hasBtn"]:
            before = det["on"]
            click_xy(pg, ".fav-btn")
            pg.wait_for_timeout(1400)
            after = pg.evaluate(
                """() => {
                  const b = document.querySelector('.fav-btn');
                  return { on: b.classList.contains('on'), label: b.innerText.replace(/\\n/g,' ').trim() };
                }"""
            )
            print("   点击前 on =", before, "→ 点击后:", json.dumps(after, ensure_ascii=False))
            print("   状态切换:", "✅" if after["on"] != before else "❌ 没变")
            shot(pg, "62-detail-fav-on.png")

            print("\n④ 关掉弹层再打开同一单，状态要还在（后端已存）")
            click_xy(pg, ".os-icon", 1)
            pg.wait_for_timeout(1200)
            click_xy(pg, ".order-card")
            pg.wait_for_timeout(2200)
            again = pg.evaluate(
                """() => {
                  const b = document.querySelector('.fav-btn');
                  return b ? { on: b.classList.contains('on'), label: b.innerText.replace(/\\n/g,' ').trim() } : null;
                }"""
            )
            print("   重开后:", json.dumps(again, ensure_ascii=False))
            print("   状态持久:", "✅" if again and again["on"] == after["on"] else "❌ 丢了")

            print("\n⑤ 再点一次还原（取消围观）")
            click_xy(pg, ".fav-btn")
            pg.wait_for_timeout(1200)
            back = pg.evaluate("() => document.querySelector('.fav-btn').classList.contains('on')")
            print("   还原:", "✅" if back == before else "❌")

        print("\n⑥ 「我的」空列表时显示当前账号")
        pg.evaluate("() => { location.hash = '#/pages/mine/index' }")
        pg.wait_for_timeout(3000)
        mine = pg.evaluate(
            """() => {
              const who = document.querySelector('.who');
              return {
                empty: !!document.querySelector('.empty') || document.querySelectorAll('.order-card').length === 0,
                hasWho: !!who,
                text: who ? who.innerText.replace(/\\n/g, ' | ') : null
              };
            }"""
        )
        print("   ", json.dumps(mine, ensure_ascii=False))
        if mine["empty"]:
            print("   空态账号自查:", "✅ 有" if mine["hasWho"] else "❌ 没有")
        shot(pg, "63-mine-who.png")

        b.close()


if __name__ == "__main__":
    main()
