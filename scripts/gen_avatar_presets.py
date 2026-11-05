"""把 AI 生成的萌粒风头像原图裁成小程序用的预设头像。

用法：python scripts/gen_avatar_presets.py
原图放 .tmp-avatars/，成品输出到 miniapp/src/static/avatars/av01.jpg ...

处理三件事：
1. 边缘各裁掉 7%——生成模型偶尔在右下角打水印，头像又会被裁成圆形，裁边后两头都干净；
2. 缩到 224px（首页/详情最大显示 116rpx，2 倍图够用）；
3. 统一转白底 JPEG，一张 20KB 左右，8 张不到 200KB，不占小程序主包。
"""

import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_DIR = os.path.join(ROOT, ".tmp-avatars")
OUT_DIR = os.path.join(ROOT, "miniapp", "src", "static", "avatars")

# 顺序即选择面板里的排列顺序，改顺序只要调这里
SOURCES = [
    ("av01", "Square_1_1_avatar_illustration_2026-11-05T03-16-44.png"),  # 短发女生抱钱包
    ("av02", "Square_1_1_avatar_illustration_2026-11-05T03-19-49.png"),  # 耳机男生
    ("av03", "Square_1_1_avatar_illustration_2026-11-05T03-17-58.png"),  # 丸子头奶茶
    ("av04", "Square_1_1_avatar_illustration_2026-11-05T03-21-25.png"),  # 卷发男生抱纸袋
    ("av05", "Square_1_1_avatar_illustration_2026-11-05T03-20-02.png"),  # 存钱罐女生
    ("av06", "Square_1_1_avatar_illustration_2026-11-05T03-18-42.png"),  # 抱笔记本男生
    ("av07", "Square_1_1_avatar_illustration_2026-11-05T03-18-53.png"),  # 双马尾蛋糕
    ("av08", "Square_1_1_avatar_illustration_2026-11-05T03-20-13.png"),  # 贝雷帽抱书
]

SIZE = 224
INSET = 0.07


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, src_name in SOURCES:
        src = os.path.join(SRC_DIR, src_name)
        if not os.path.exists(src):
            print(f"跳过（原图不存在）: {name} <- {src_name}")
            continue
        im = Image.open(src).convert("RGB")
        w, h = im.size
        dx, dy = int(w * INSET), int(h * INSET)
        im = im.crop((dx, dy, w - dx, h - dy)).resize((SIZE, SIZE), Image.LANCZOS)
        out = os.path.join(OUT_DIR, f"{name}.jpg")
        im.save(out, "JPEG", quality=90, optimize=True, progressive=True)
        print(f"{name}.jpg  {os.path.getsize(out) // 1024}KB")


if __name__ == "__main__":
    main()
