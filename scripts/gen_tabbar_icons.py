"""生成小程序 tabBar 图标（81x81 PNG，透明底）。

风格：统一的粗圆头线条 + 至少一处曲线，走「圆润可爱」路线。
尺寸按微信 tabBar 建议的 81x81，4 倍超采样后 LANCZOS 缩小做抗锯齿。

用法：python scripts/gen_tabbar_icons.py
输出：miniapp/src/static/tabbar/*.png
"""

import math
import os

from PIL import Image, ImageDraw

SIZE = 81
S = 4  # 超采样倍数
GRAY = (154, 160, 166, 255)  # #9AA0A6 未选中
ORANGE = (255, 107, 53, 255)  # #FF6B35 选中

OUT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "miniapp",
    "src",
    "static",
    "tabbar",
)


def _ellipse(d, cx, cy, r, color, fill=True, width=1):
    box = [(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S]
    if fill:
        d.ellipse(box, fill=color)
    else:
        d.ellipse(box, outline=color, width=max(1, int(round(width * S))))


def poly(d, pts, w, color):
    """折线，每段单独画 + 每个顶点补圆点，模拟 round cap / round join。"""
    ww = max(1, int(round(w * S)))
    scaled = [(x * S, y * S) for x, y in pts]
    for i in range(len(scaled) - 1):
        d.line([scaled[i], scaled[i + 1]], fill=color, width=ww)
    for x, y in scaled:
        r = ww / 2
        d.ellipse([x - r, y - r, x + r, y + r], fill=color)


def arc(d, c, r, a0, a1, w, color):
    """圆弧 + 两端圆头。角度按 PIL 约定：0° 在 3 点方向，顺时针（y 轴向下）。"""
    ww = max(1, int(round(w * S)))
    d.arc(
        [(c[0] - r) * S, (c[1] - r) * S, (c[0] + r) * S, (c[1] + r) * S],
        a0,
        a1,
        fill=color,
        width=ww,
    )
    for a in (a0, a1):
        px = c[0] + r * math.cos(math.radians(a))
        py = c[1] + r * math.sin(math.radians(a))
        rr = ww / 2
        d.ellipse([px * S - rr, py * S - rr, px * S + rr, py * S + rr], fill=color)


def rrect(d, box, radius, w, color):
    d.rounded_rectangle(
        [box[0] * S, box[1] * S, box[2] * S, box[3] * S],
        radius=radius * S,
        outline=color,
        width=max(1, int(round(w * S))),
    )


def circle(d, c, r, w, color):
    _ellipse(d, c[0], c[1], r, color, fill=False, width=w)


# ---------- 四个图标 ----------


def icon_square(d, c):
    """广场：小房子（拱形门，屋顶出檐）"""
    poly(d, [(11, 38), (40.5, 14), (70, 38)], 7, c)  # 屋顶
    poly(d, [(21, 30), (21, 61), (60, 61), (60, 30)], 7, c)  # 屋身
    poly(d, [(31, 61), (31, 51)], 6, c)  # 门左框
    arc(d, (40.5, 51), 9.5, 180, 360, 6, c)  # 拱门
    poly(d, [(50, 51), (50, 61)], 6, c)  # 门右框


def icon_publish(d, c):
    """发一个：圆角方框 + 粗加号（缩到 24px 显示，装饰性小星会糊成脏点，所以不加）"""
    rrect(d, (11, 17, 67, 73), 18, 7.5, c)
    poly(d, [(27, 45), (51, 45)], 7.5, c)
    poly(d, [(39, 33), (39, 57)], 7.5, c)


def icon_group(d, c):
    """审批群：两个圆脑袋 + 圆弧肩。肩膀宽度约等于头径 1.6 倍才不像「头大身小」，
    所以两肩必然要挨在一起，靠弧线的走向区分，不必强行留缝。"""
    circle(d, (25, 27), 10.5, 7, c)
    arc(d, (25, 58), 16, 168, 372, 7, c)
    circle(d, (57, 29), 9.5, 6.5, c)
    arc(d, (57, 58), 14.5, 168, 372, 6.5, c)


def icon_mine(d, c):
    """我的：单个圆脑袋 + 大圆弧肩"""
    circle(d, (40.5, 27), 12.5, 7, c)
    arc(d, (40.5, 67), 23, 168, 372, 7, c)


ICONS = [
    ("tab-square", icon_square),
    ("tab-publish", icon_publish),
    ("tab-group", icon_group),
    ("tab-mine", icon_mine),
]


def render(fn, color):
    img = Image.new("RGBA", (SIZE * S, SIZE * S), (0, 0, 0, 0))
    fn(ImageDraw.Draw(img), color)
    return img.resize((SIZE, SIZE), Image.LANCZOS)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, fn in ICONS:
        render(fn, GRAY).save(os.path.join(OUT_DIR, f"{name}.png"))
        render(fn, ORANGE).save(os.path.join(OUT_DIR, f"{name}-active.png"))
        print("wrote", name)
    # 预览图：2x4 拼一张，方便肉眼比对（放 static 外，免得被打包进小程序）
    pad, cell = 12, SIZE
    sheet = Image.new("RGBA", (cell * 4 + pad * 5, cell * 2 + pad * 3), (255, 255, 255, 255))
    for i, (name, fn) in enumerate(ICONS):
        sheet.alpha_composite(render(fn, GRAY), (pad + i * (cell + pad), pad))
        sheet.alpha_composite(render(fn, ORANGE), (pad + i * (cell + pad), pad * 2 + cell))
    prev = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_tabbar_preview.png")
    sheet.save(prev)
    print("preview", prev)


if __name__ == "__main__":
    main()
