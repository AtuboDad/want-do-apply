# -*- coding: utf-8 -*-
"""本轮四项改动的验收脚本。

1. 「我的 → 我提交的」列表条数与顶部统计是否一致
2. 详情弹层在长内容下能不能滚动（滚轮 + 触摸拖动），页面会不会跟着动
3. 卡片上的围观按钮在不在、点了有没有反应
4. 头像选择面板是不是 20 个

用法：python scripts/_verify_round.py
"""

import json
import os
import time

from playwright.sync_api import sync_playwright

BASE = "http://localhost:5175"
OUT = ".playwright-cli/final"

JS_SHEET = r"""
() => {
  const root = document.querySelector('.panel-scroll');
  if (!root) return null;
  let best = null;
  for (const el of root.querySelectorAll('*')) {
    if (el.clientHeight === 0) continue;
    if (el.scrollHeight <= el.clientHeight + 1) continue;
    const oy = getComputedStyle(el).overflowY;
    if (oy !== 'auto' && oy !== 'scroll') continue;
    best = el;
  }
  return best ? { top: Math.round(best.scrollTop), sh: best.scrollHeight, ch: best.clientHeight } : null;
}
"""

JS_PAGE = r"""
() => ({
  y: Math.round(window.scrollY),
  htmlTop: Math.round(document.documentElement.scrollTop),
  htmlOF: document.documentElement.style.overflow,
  bodyOF: document.body.style.overflow,
  sheetOpen: !!document.querySelector('.panel-scroll')
})
"""

JS_CARD_POINT = r"""
() => {
  const vh = window.innerHeight;
  for (const el of document.querySelectorAll('.order-card')) {
    const r = el.getBoundingClientRect();
    if (r.top > 120 && r.bottom < vh - 80) {
      return { x: Math.round(r.left + r.width / 2), y: Math.round(r.top + Math.min(60, r.height / 2)) };
    }
  }
  return null;
}
"""


def shot(pg, name):
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name)
    pg.screenshot(path=path)
    print(f"   截图 {path}")


def main():
    with sync_playwright() as p:
        b = p.chromium.launch(headless=True, channel="chrome")
        ctx = b.new_context(
            viewport={"width": 390, "height": 844},
            device_scale_factor=2,
            has_touch=True,
            is_mobile=True,
        )
        # 固定成 qa-mine 这个账号（造数据脚本里种的两条单都在它名下）
        ctx.add_init_script(
            "localStorage.setItem('wda_dev_id','qa-mine');"
            "localStorage.removeItem('wda_token');"
            "localStorage.removeItem('wda_user');"
        )
        pg = ctx.new_page()
        reqs = []
        pg.on("request", lambda q: reqs.append(q.url) if "/app/order/mine" in q.url else None)
        pg.goto(BASE + "/#/pages/mine/index", wait_until="networkidle")
        pg.wait_for_timeout(2500)

        print("① 「我的 → 我提交的」")
        info = pg.evaluate(
            """() => ({
              nick: document.querySelector('.nick')?.innerText.trim(),
              stats: [...document.querySelectorAll('.stat')].map(s => ({
                k: s.querySelector('.label').innerText.trim(),
                v: s.querySelector('.num').innerText.trim()
              })),
              cards: [...document.querySelectorAll('.order-card')].map(c => ({
                title: c.querySelector('.title')?.innerText.trim(),
                status: c.querySelector('.status')?.innerText.trim(),
                fav: c.querySelector('.fav-text')?.innerText.trim()
              })),
              tab: document.querySelector('.tab.active')?.innerText.trim(),
              pageY: Math.round(window.scrollY)
            })"""
        )
        print("   昵称:", info["nick"], "| tab:", info["tab"])
        print("   顶部统计:", json.dumps(info["stats"], ensure_ascii=False))
        print(f"   列表条数: {len(info['cards'])}")
        for c in info["cards"]:
            print(f"     - {c['title']} | {c['status']} | 围观按钮: {c['fav']}")
        shot(pg, "20-mine-apply.png")

        print("\n①b 再点一下「我提交的」（当前已经选中的 tab）")
        reqs.clear()
        pg.evaluate(
            """() => {
              const el = [...document.querySelectorAll('.tab')].find(t => t.innerText.trim() === '我提交的');
              el.click();
            }"""
        )
        time.sleep(1.8)
        print(f"   触发 /app/order/mine 请求：{len(reqs)} 次")
        print("   刷新生效:", "✅" if len(reqs) >= 1 else "❌ 还是不理人")

        print("\n② 打开长理由那单，验证弹层滚动")
        # 第一张卡就是长理由单（create_time 最新）
        pt = pg.evaluate(JS_CARD_POINT)
        pg.mouse.click(pt["x"], pt["y"])
        pg.wait_for_timeout(1500)
        s0 = pg.evaluate(JS_PAGE)
        sh0 = pg.evaluate(JS_SHEET)
        print("   打开后 页面:", json.dumps(s0, ensure_ascii=False))
        print("   弹层滚动区:", json.dumps(sh0, ensure_ascii=False))
        shot(pg, "21-sheet-long-top.png")

        print("   a) 鼠标停在遮罩上方空白处滚 600px（最易穿透）")
        pg.mouse.move(195, 40)
        pg.mouse.wheel(0, 600)
        pg.wait_for_timeout(700)
        s1 = pg.evaluate(JS_PAGE)
        sh1 = pg.evaluate(JS_SHEET)
        print("      页面:", json.dumps(s1, ensure_ascii=False))
        print("      弹层:", json.dumps(sh1, ensure_ascii=False))
        print("      页面没动:", "✅" if s1["y"] == s0["y"] else f"❌ {s0['y']} -> {s1['y']}")

        print("   b) 鼠标在弹层内滚 400px")
        pg.mouse.move(195, 500)
        pg.mouse.wheel(0, 400)
        pg.wait_for_timeout(700)
        s2 = pg.evaluate(JS_PAGE)
        sh2 = pg.evaluate(JS_SHEET)
        print("      弹层:", json.dumps(sh2, ensure_ascii=False))
        print("      页面:", json.dumps(s2, ensure_ascii=False))
        print("      弹层能滚:", "✅" if sh2 and sh2["top"] > (sh1["top"] if sh1 else 0) else "❌")
        shot(pg, "22-sheet-long-scrolled.png")

        print("   c) 触摸拖动（模拟手指上滑 300px）")
        before = pg.evaluate(JS_SHEET)
        cdp = ctx.new_cdp_session(pg)
        cdp.send(
            "Input.dispatchTouchEvent",
            {
                "type": "touchStart",
                "touchPoints": [{"x": 195, "y": 600}],
            },
        )
        for i in range(1, 13):
            cdp.send(
                "Input.dispatchTouchEvent",
                {
                    "type": "touchMove",
                    "touchPoints": [{"x": 195, "y": 600 - i * 25}],
                },
            )
            time.sleep(0.02)
        cdp.send("Input.dispatchTouchEvent", {"type": "touchEnd", "touchPoints": []})
        pg.wait_for_timeout(800)
        after = pg.evaluate(JS_SHEET)
        s3 = pg.evaluate(JS_PAGE)
        print("      弹层:", before, "->", after)
        print("      页面:", json.dumps(s3, ensure_ascii=False))
        print("      拖动只动弹层:", "✅" if after and before and after["top"] > before["top"] and s3["y"] == s0["y"] else "❌")
        shot(pg, "23-sheet-touch.png")

        print("\n③ 关掉弹层，检查页面位置复原")
        pg.mouse.click(195, 24)
        pg.wait_for_timeout(1200)
        s4 = pg.evaluate(JS_PAGE)
        print("   ", json.dumps(s4, ensure_ascii=False))

        print("\n④ 点卡片上的「围观」按钮")
        fav_box = pg.locator(".fav").first.bounding_box()
        before_txt = pg.locator(".fav-text").first.inner_text()
        pg.mouse.click(fav_box["x"] + fav_box["width"] / 2, fav_box["y"] + fav_box["height"] / 2)
        pg.wait_for_timeout(1500)
        after_txt = pg.locator(".fav-text").first.inner_text()
        print(f"   点前：{before_txt} → 点后：{after_txt}")
        print("   状态翻转:", "✅" if before_txt != after_txt else "❌ 没变")
        shot(pg, "24-fav-clicked.png")
        # 再点一次复原，别把数据留脏
        pg.mouse.click(fav_box["x"] + fav_box["width"] / 2, fav_box["y"] + fav_box["height"] / 2)
        pg.wait_for_timeout(1200)
        print("   再点一次复原:", pg.locator(".fav-text").first.inner_text())

        print("\n⑤ 点头像看 20 个预设")
        ab = pg.locator(".avatar").first.bounding_box()
        pg.mouse.click(ab["x"] + ab["width"] / 2, ab["y"] + ab["height"] / 2)
        pg.wait_for_timeout(1500)
        av = pg.evaluate(
            """() => {
              const cells = [...document.querySelectorAll('.av-cell')];
              const box = document.querySelector('.avatar-scroll');
              return {
                count: cells.length,
                first: cells[0]?.querySelector('image, img')?.getAttribute('src'),
                last: cells[cells.length - 1]?.querySelector('image, img')?.getAttribute('src'),
                box: box ? { h: Math.round(box.getBoundingClientRect().height), sh: box.scrollHeight } : null
              };
            }"""
        )
        print("   头像数量:", av["count"], "| 首张:", av["first"], "| 末张:", av["last"])
        print("   滚动容器:", json.dumps(av["box"], ensure_ascii=False))
        shot(pg, "25-avatar-20.png")

        b.close()


if __name__ == "__main__":
    main()
