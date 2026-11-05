# -*- coding: utf-8 -*-
"""
滚动穿透 + 滚动位置恢复 —— 正确版测试。

要点：不能用 locator.click()（Playwright 会自动 scrollIntoView，把页面滚回顶部，
造成「关闭后跳回顶部」的假象）。一律用鼠标坐标点当前可见的元素。
"""
import json

from playwright.sync_api import sync_playwright

BASE = "http://localhost:5175"

# 用与 scroll-lock.ts 里 pickScroller 一致的逻辑量弹层真身
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
  return best ? { top: best.scrollTop, sh: best.scrollHeight, ch: best.clientHeight } : null;
}
"""

# 可见卡片：取一个当前视口内的卡片中心点
JS_CARD_POINT = r"""
() => {
  const vh = window.innerHeight;
  for (const el of document.querySelectorAll('.order-card')) {
    const r = el.getBoundingClientRect();
    if (r.top > 120 && r.bottom < vh - 80) {
      return { x: Math.round(r.left + r.width / 2), y: Math.round(r.top + r.height / 2), top: Math.round(r.top) };
    }
  }
  return null;
}
"""


def state(pg):
    return pg.evaluate(
        "() => ({ y: window.scrollY, htmlTop: document.documentElement.scrollTop,"
        " bodyTop: document.body.scrollTop,"
        " htmlOF: document.documentElement.style.overflow,"
        " bodyOF: document.body.style.overflow,"
        " sheet: (document.querySelector('.panel-scroll') ? 1 : 0) })"
    )


with sync_playwright() as p:
    b = p.chromium.launch(headless=True, channel="chrome")
    ctx = b.new_context(viewport={"width": 390, "height": 844}, device_scale_factor=2)
    pg = ctx.new_page()
    pg.goto(BASE + "/#/pages/index/index", wait_until="networkidle")
    pg.wait_for_timeout(2500)

    print("① 滚轮把页面滚下去")
    pg.mouse.move(195, 420)
    pg.mouse.wheel(0, 600)
    pg.wait_for_timeout(500)
    base = state(pg)
    print("   ", json.dumps(base, ensure_ascii=False))
    y0 = base["y"]

    print("② 用坐标点一张当前可见的卡片（不用 locator.click，避免自动 scrollIntoView）")
    pt = pg.evaluate(JS_CARD_POINT)
    print("   卡片中心点：", pt)
    pg.mouse.click(pt["x"], pt["y"])
    pg.wait_for_timeout(1200)
    s = state(pg)
    print("   ", json.dumps(s, ensure_ascii=False), "  ← 弹层打开后")
    print("   打开瞬间是否还在原位:", "是" if s["y"] == y0 or s["y"] == 0 else f"否（{y0} -> {s['y']}）")

    print("③ 鼠标停在遮罩顶部空白处，滚轮 500px（最容易穿透的位置）")
    pg.mouse.move(195, 60)
    pg.mouse.wheel(0, 500)
    pg.wait_for_timeout(600)
    print("   弹层内滚动区:", json.dumps(pg.evaluate(JS_SHEET), ensure_ascii=False))
    s2 = state(pg)
    print("   页面:", json.dumps(s2, ensure_ascii=False))
    print("   页面是否纹丝不动:", "✅ 是" if s2["y"] == s["y"] else f"❌ 否 ({s['y']} -> {s2['y']})")

    print("④ 鼠标挪到弹层内部，滚轮 400px")
    pg.mouse.move(195, 500)
    pg.mouse.wheel(0, 400)
    pg.wait_for_timeout(600)
    sheet = pg.evaluate(JS_SHEET)
    s3 = state(pg)
    print("   弹层内滚动区:", json.dumps(sheet, ensure_ascii=False))
    print("   页面滚轮量:", s["y"], "->", s3["y"], "（应保持不变）")

    print("⑤ 点遮罩顶部关闭弹层，检查页面位置是否复原")
    pg.mouse.click(195, 24)
    pg.wait_for_timeout(1500)
    s4 = state(pg)
    print("   ", json.dumps(s4, ensure_ascii=False))
    print("   弹层已关闭:", "✅" if s4["sheet"] == 0 else "❌ 还开着")
    print("   overflow 已复原:", "✅" if s4["htmlOF"] == "" and s4["bodyOF"] == "" else "❌")
    print("   滚动位置复原:", "✅ 回到", y0 if s4["y"] == y0 else f"❌ 期望 {y0}，实际 {s4['y']}")

    b.close()
