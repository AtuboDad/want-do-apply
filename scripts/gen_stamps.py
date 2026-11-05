"""生成审批印章 PNG（透明底）。

三枚：通过（墨绿）、驳回（砖红）、已结束（灰）。
画法：3 倍超采样再缩小，边缘才不会有毛刺；外圈 + 内圈双线，顶部弧形拉丁字，
中间带框的中文结论，底部三颗星 —— 对齐参考图里那枚橡皮章。
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "miniapp/src/static/stamps"
OUT.mkdir(parents=True, exist_ok=True)

S = 3                    # 超采样倍数
CANVAS = 300             # 最终尺寸
W = CANVAS * S
C = W // 2

FONT_CN = "C:/Windows/Fonts/msyhbd.ttc"
FONT_EN = "C:/Windows/Fonts/arialbd.ttf"

VARIANTS = [
    ("pass", "APPROVED", "已通过", (63, 122, 90)),
    ("reject", "REJECTED", "已驳回", (176, 84, 63)),
    ("closed", "CLOSED", "已结束", (140, 133, 120)),
    ("cancel", "WITHDRAWN", "已撤回", (150, 143, 130)),
]


def draw_arc_text(img: Image.Image, text: str, cx: int, cy: int, radius: int,
                  font: ImageFont.FreeTypeFont, color, spread: float = 46.0) -> None:
    """沿上半圆弧排字：spread 是整段文字覆盖的角度范围（度）。"""
    n = len(text)
    for i, ch in enumerate(text):
        t = 0.0 if n == 1 else (i - (n - 1) / 2) / ((n - 1) / 2)
        angle = 90 - t * spread            # 90 = 正上方，t 从 -1 到 1 即从左到右
        rad = math.radians(angle)
        x = cx + radius * math.cos(rad)
        y = cy - radius * math.sin(rad)
        rot = angle - 90                   # 沿切线方向转：PIL 正角度是逆时针

        box = font.getbbox(ch)
        cw, chh = box[2] - box[0], box[3] - box[1]
        pad = 12
        tile = Image.new("RGBA", (cw + pad * 2, chh + pad * 2), (0, 0, 0, 0))
        d = ImageDraw.Draw(tile)
        d.text((pad - box[0], pad - box[1]), ch, font=font, fill=color)
        # 用 Image.rotate 的 expand=True 保持字形完整
        tile = tile.rotate(rot, resample=Image.BICUBIC, expand=True)
        img.alpha_composite(tile, (int(x - tile.width / 2), int(y - tile.height / 2)))


def make_star(draw: ImageDraw.ImageDraw, cx: float, cy: float, r: float, color) -> None:
    pts = []
    for i in range(10):
        ang = -math.pi / 2 + i * math.pi / 5
        rr = r if i % 2 == 0 else r * 0.42
        pts.append((cx + rr * math.cos(ang), cy + rr * math.sin(ang)))
    draw.polygon(pts, fill=color)


def build(name: str, top_text: str, cn: str, rgb) -> None:
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    color = rgb + (255,)

    # 外圈 + 内圈
    d.ellipse([C - 432, C - 432, C + 432, C + 432], outline=color, width=24)
    d.ellipse([C - 372, C - 372, C + 372, C + 372], outline=color, width=9)

    # 顶部弧形英文
    f_en = ImageFont.truetype(FONT_EN, 56)
    draw_arc_text(img, top_text, C, C, 316, f_en, color, spread=96)

    # 中间中文 + 方框
    f_cn = ImageFont.truetype(FONT_CN, 148)
    box = d.textbbox((0, 0), cn, font=f_cn)
    tw, th = box[2] - box[0], box[3] - box[1]
    pad_x, pad_y = 26, 14
    x0, y0 = C - tw / 2 - pad_x, C - th / 2 - pad_y
    x1, y1 = C + tw / 2 + pad_x, C + th / 2 + pad_y
    d.rounded_rectangle([x0, y0, x1, y1], radius=16, outline=color, width=9)
    d.text((C - tw / 2 - box[0], C - th / 2 - box[1]), cn, font=f_cn, fill=color)

    # 底部三颗星
    for i in (-1, 0, 1):
        make_star(d, C + i * 104, C + 238, 40, color)

    img = img.resize((CANVAS, CANVAS), Image.LANCZOS)
    path = OUT / f"stamp-{name}.png"
    img.save(path)
    print("saved", path)


for name, top, cn, rgb in VARIANTS:
    build(name, top, cn, rgb)
