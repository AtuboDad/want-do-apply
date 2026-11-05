#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""UI 验证（第五轮）：
① 修改资料弹层不再被顶出屏幕顶部（sheet 顶边 y > 0，高度 <= 视口）
② 头像分组做成标签切换：默认「我们画的」20 个，切「会员专属」只剩 6 个
③ 审批详情弹层与底部菜单栏之间的间距 >= 24px
"""
import json

from playwright.sync_api import sync_playwright

BASE = "http://localhost:5175"
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
SHOT = ".playwright-cli/final"


def shot(pg, name):
    pg.screenshot(path=f"{SHOT}/{name}")
    print("   截图:", name)


def click_el(pg, sel, nth=0):
    r = pg.evaluate(
        """([sel, nth]) => {
          const els = document.querySelectorAll(sel);
          const el = els[nth];
          if (!el) return null;
          const b = el.getBoundingClientRect();
          return { x: Math.round(b.x + b.width / 2), y: Math.round(b.y + b.height / 2) };
        }""",
        [sel, nth],
    )
    if not r:
        return False
    pg.mouse.click(r["x"], r["y"])
    return True


def sheet_geom(pg):
    return pg.evaluate(
        """() => {
          const sheet = document.querySelector('.sheet');
          if (!sheet) return null;
          const s = sheet.getBoundingClientRect();
          const body = document.querySelector('.sheet-body');
          // scroll-view 真身是内层 overflow-y:auto 的那个
          let inner = null;
          if (body) {
            for (const el of body.querySelectorAll('*')) {
              const oy = getComputedStyle(el).overflowY;
              if ((oy === 'auto' || oy === 'scroll') && el.clientHeight > 0) inner = el;
            }
          }
          return {
            top: Math.round(s.top),
            bottom: Math.round(s.bottom),
            height: Math.round(s.height),
            viewportH: window.innerHeight,
            bodyH: inner ? Math.round(inner.clientHeight) : null,
            bodyScrollH: inner ? Math.round(inner.scrollHeight) : null,
            canScroll: inner ? inner.scrollHeight > inner.clientHeight + 2 : null
          };
        }"""
    )


def main():
    with sync_playwright() as p:
        b = p.chromium.launch(headless=True, executable_path=CHROME)
        ctx = b.new_context(viewport={"width": 390, "height": 844}, device_scale_factor=2)
        pg = ctx.new_page()

        pg.goto(BASE + "/#/pages/mine/index", wait_until="networkidle")
        pg.wait_for_timeout(2600)

        print("① 打开「修改资料」→ 弹层是否顶出屏幕")
        click_el(pg, ".avatar")
        pg.wait_for_timeout(1200)
        g1 = sheet_geom(pg)
        print("   ", json.dumps(g1, ensure_ascii=False))
        ok1 = g1 and g1["top"] >= 8 and g1["bottom"] <= g1["viewportH"]
        print("   顶部没被切:", "✅" if ok1 else "❌")
        shot(pg, "50-profile-tab-preset.png")

        print("\n② 标签：默认「我们画的」")
        t1 = pg.evaluate(
            """() => ({
                 tabs: [...document.querySelectorAll('.av-tab')].map(t => t.innerText.replace(/\\n/g, ' ').trim()),
                 activeIdx: [...document.querySelectorAll('.av-tab')].findIndex(t => t.classList.contains('on')),
                 cells: document.querySelectorAll('.av-cell').length,
                 vipCells: document.querySelectorAll('.vip-cell').length
               })"""
        )
        print("   ", json.dumps(t1, ensure_ascii=False))
        ok2 = t1["cells"] == 20 and t1["vipCells"] == 0 and t1["activeIdx"] == 0
        print("   普通组 20 个:", "✅" if ok2 else "❌")

        print("\n③ 切到「会员专属」")
        click_el(pg, ".av-tab", 1)
        pg.wait_for_timeout(600)
        t2 = pg.evaluate(
            """() => ({
                 activeIdx: [...document.querySelectorAll('.av-tab')].findIndex(t => t.classList.contains('on')),
                 cells: document.querySelectorAll('.av-cell').length,
                 vipCells: document.querySelectorAll('.vip-cell').length,
                 badges: document.querySelectorAll('.vip-cell .vip').length,
                 hasToggle: !!document.querySelector('.vip-toggle')
               })"""
        )
        print("   ", json.dumps(t2, ensure_ascii=False))
        ok3 = t2["vipCells"] == 6 and t2["cells"] == 6 and t2["activeIdx"] == 1 and t2["hasToggle"]
        print("   会员组 6 个 + 开关:", "✅" if ok3 else "❌")
        g2 = sheet_geom(pg)
        print("   弹层几何:", json.dumps(g2, ensure_ascii=False))
        shot(pg, "51-profile-tab-vip.png")

        print("\n④ 切回「我们画的」，中间区能滚吗")
        click_el(pg, ".av-tab", 0)
        pg.wait_for_timeout(500)
        g3 = sheet_geom(pg)
        print("   ", json.dumps(g3, ensure_ascii=False))
        print("   头像区可滚:", "✅" if g3 and g3["canScroll"] else "（内容未超高，无需滚）")

        print("\n⑤ 小屏（iPhone SE 375×667）再看一次")
        pg.set_viewport_size({"width": 375, "height": 667})
        pg.wait_for_timeout(800)
        g4 = sheet_geom(pg)
        print("   ", json.dumps(g4, ensure_ascii=False))
        ok4 = g4 and g4["top"] >= 8 and g4["bottom"] <= g4["viewportH"]
        print("   小屏也不顶出去:", "✅" if ok4 else "❌")
        shot(pg, "52-profile-se.png")
        pg.set_viewport_size({"width": 390, "height": 844})
        pg.wait_for_timeout(600)
        click_el(pg, ".sheet-close")
        pg.wait_for_timeout(700)

        print("\n⑥ 审批详情弹层与底部菜单的间距")
        pg.evaluate("() => { location.hash = '#/pages/index/index' }")
        pg.wait_for_timeout(2800)
        cards = pg.evaluate(
            """() => [...document.querySelectorAll('.order-card')].map(c => {
                 const r = c.getBoundingClientRect();
                 return { x: Math.round(r.x + r.width/2), y: Math.round(r.y + r.height/2) };
               }).filter(c => c.y > 140 && c.y < 700)"""
        )
        print("   可点卡片数:", len(cards))
        if cards:
            pg.mouse.click(cards[0]["x"], cards[0]["y"])
        pg.wait_for_timeout(2200)
        gap = pg.evaluate(
            """() => {
              const sheet = document.querySelector('.os-sheet');
              const tabbar = document.querySelector('uni-tabbar');
              if (!sheet) return null;
              const s = sheet.getBoundingClientRect();
              // uni-tabbar 是 fixed 容器，自身高度可能是 0，量内部真实高度
              const th = tabbar ? (tabbar.offsetHeight || tabbar.getBoundingClientRect().height) : 0;
              const inner = tabbar ? tabbar.querySelector('.uni-tabbar') : null;
              const th2 = inner ? inner.getBoundingClientRect().height : 0;
              const h = Math.max(th, th2);
              return {
                sheetBottom: Math.round(s.bottom),
                sheetH: Math.round(s.height),
                tabbarH: Math.round(h),
                tabbarTop: h ? Math.round(window.innerHeight - h) : null,
                gap: h ? Math.round(window.innerHeight - h - s.bottom) : null,
                viewportH: window.innerHeight
              };
            }"""
        )
        print("   ", json.dumps(gap, ensure_ascii=False))
        if gap and gap["gap"] is not None:
            ok5 = gap["gap"] >= 24
            print("   底部留白:", f"{gap['gap']}px", "✅" if ok5 else "❌ 太贴了")
        shot(pg, "53-sheet-gap.png")

        b.close()


if __name__ == "__main__":
    main()
