#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""UI 验证：会员头像面板 / VIP 角标 / 弹层与 tabBar 的间距。

用真实坐标点击（uni-app 的 @tap 不认 element.click()）。
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
    """按元素真实坐标点一下；返回是否点到了。"""
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


def main():
    with sync_playwright() as p:
        b = p.chromium.launch(headless=True, executable_path=CHROME)
        ctx = b.new_context(viewport={"width": 390, "height": 844}, device_scale_factor=2)
        pg = ctx.new_page()

        pg.goto(BASE + "/#/pages/mine/index", wait_until="networkidle")
        pg.wait_for_timeout(2600)

        print("① 打开「修改资料」")
        click_el(pg, ".avatar")
        pg.wait_for_timeout(1200)
        info = pg.evaluate(
            """() => {
              const cells = [...document.querySelectorAll('.av-cell')];
              const vipCells = [...document.querySelectorAll('.vip-cell')];
              const badges = [...document.querySelectorAll('.vip-cell .vip')];
              const plain = cells.filter(c => !c.classList.contains('vip-cell'));
              const sheet = document.querySelector('.sheet');
              return {
                total: cells.length,
                plain: plain.length,
                vip: vipCells.length,
                badges: badges.length,
                hasToggle: !!document.querySelector('.vip-toggle'),
                toggleOn: !!document.querySelector('.toggle.on'),
                panelOpen: !!sheet,
                sheetH: sheet ? Math.round(sheet.getBoundingClientRect().height) : null,
                viewportH: window.innerHeight
              };
            }"""
        )
        print("   ", json.dumps(info, ensure_ascii=False))
        ok = info["plain"] == 20 and info["vip"] == 6 and info["badges"] == 6 and info["hasToggle"]
        print("   面板完整性:", "✅" if ok else "❌")
        shot(pg, "40-vip-panel.png")

        print("\n② 会员组滚到视口中间")
        pg.evaluate("() => document.querySelector('.vip-grid').scrollIntoView({block:'center'})")
        pg.wait_for_timeout(600)
        shot(pg, "41-vip-grid.png")

        print("\n③ 选一个会员头像并保存")
        click_el(pg, ".vip-cell .av-img")
        pg.wait_for_timeout(500)
        click_el(pg, ".submit")
        pg.wait_for_timeout(2200)
        top = pg.evaluate(
            """() => {
              const wrap = document.querySelector('.avatar-wrap');
              return {
                hasBadge: !!wrap?.querySelector('.vip'),
                badgeText: wrap?.querySelector('.vip-text')?.innerText || '',
                avatarSrc: wrap?.querySelector('img')?.getAttribute('src') || ''
              };
            }"""
        )
        print("   顶部头像:", json.dumps(top, ensure_ascii=False))
        good = top["hasBadge"] and top["badgeText"] == "VIP" and "av2" in top["avatarSrc"]
        print("   VIP 角标:", "✅ 显示" if good else "❌")
        shot(pg, "42-mine-vip.png")

        print("\n④ 关掉「显示角标」")
        click_el(pg, ".avatar")
        pg.wait_for_timeout(1200)
        click_el(pg, ".vip-toggle")
        pg.wait_for_timeout(1800)
        after = pg.evaluate(
            """() => ({
                 toggleOn: !!document.querySelector('.toggle.on'),
                 badgesInGrid: document.querySelectorAll('.vip-cell .vip').length
               })"""
        )
        print("   弹层内:", json.dumps(after, ensure_ascii=False))
        click_el(pg, ".sheet-close")
        pg.wait_for_timeout(900)
        still = pg.evaluate("() => !!document.querySelector('.avatar-wrap .vip')")
        print("   顶部角标:", "✅ 已隐藏" if not still else "❌ 还在")
        shot(pg, "43-mine-vip-off.png")

        print("\n④b 再把开关打开（还原）")
        click_el(pg, ".avatar")
        pg.wait_for_timeout(1200)
        click_el(pg, ".vip-toggle")
        pg.wait_for_timeout(1600)
        click_el(pg, ".sheet-close")
        pg.wait_for_timeout(800)
        back = pg.evaluate("() => !!document.querySelector('.avatar-wrap .vip')")
        print("   顶部角标:", "✅ 回来了" if back else "❌ 没回来")

        print("\n⑤ 详情弹层与底部菜单的间距")
        pg.evaluate("() => { location.hash = '#/pages/index/index' }")
        pg.wait_for_timeout(2800)
        cards = pg.evaluate(
            """() => [...document.querySelectorAll('.order-card')].map(c => {
                 const r = c.getBoundingClientRect();
                 return { x: Math.round(r.x + r.width/2), y: Math.round(r.y + r.height/2) };
               }).filter(c => c.y > 140 && c.y < 760)"""
        )
        print("   可点卡片数:", len(cards))
        clicked = False
        if cards:
            pg.mouse.click(cards[0]["x"], cards[0]["y"])
            clicked = True
        pg.wait_for_timeout(2000)
        gap = pg.evaluate(
            """() => {
              const sheet = document.querySelector('.os-sheet');
              const tabbar = document.querySelector('uni-tabbar');
              if (!sheet) return null;
              const s = sheet.getBoundingClientRect();
              const t = tabbar ? tabbar.getBoundingClientRect() : null;
              return {
                sheetBottom: Math.round(s.bottom),
                sheetH: Math.round(s.height),
                tabbarTop: t ? Math.round(t.top) : null,
                gap: t ? Math.round(t.top - s.bottom) : null,
                viewportH: window.innerHeight
              };
            }"""
        )
        print("   间距:", json.dumps(gap, ensure_ascii=False))
        if gap and gap["gap"] is not None:
            print("   底部留白:", f"{gap['gap']}px", "✅" if gap["gap"] >= 8 else "❌ 太贴了")
        shot(pg, "44-sheet-gap.png")
        print("   卡片点开了吗:", clicked)

        b.close()


if __name__ == "__main__":
    main()
