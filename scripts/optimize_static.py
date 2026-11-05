"""
小程序静态资源瘦身。

只做两类处理：
- 印章 PNG（300x300 RGBA，纯色 + 透明）：缩到 264（3 倍屏下按 168rpx 显示够用）+ 128 色量化。
  无损观感，单张 45K -> 13K。
- 头像 JPG：quality 88 + progressive，视觉几乎无损。

超过 200K 的单文件必须走 CDN（微信建议），这里顺带在结尾做一次体检。
"""
from PIL import Image
import os
import glob

ROOT = os.path.join(os.path.dirname(__file__), '..', 'miniapp', 'src', 'static')


def kb(n):
    return n / 1024


def do_stamps():
    total_before = total_after = 0
    for f in glob.glob(os.path.join(ROOT, 'stamps', '*.png')):
        before = os.path.getsize(f)
        im = Image.open(f).convert('RGBA')
        if im.size[0] > 264:
            im = im.resize((264, 264), Image.LANCZOS)
        # 印章只有两三种颜色，128 色足够，透明通道保留
        im = im.quantize(colors=128, method=Image.FASTOCTREE)
        im.save(f, 'PNG', optimize=True)
        after = os.path.getsize(f)
        total_before += before
        total_after += after
        print(f'  {os.path.basename(f):20s} {kb(before):6.1f}K -> {kb(after):5.1f}K')
    print(f'  印章合计 {kb(total_before):.0f}K -> {kb(total_after):.0f}K')


def do_avatars():
    total_before = total_after = 0
    for f in glob.glob(os.path.join(ROOT, 'avatars', '*.jpg')):
        before = os.path.getsize(f)
        im = Image.open(f).convert('RGB')
        im.save(f, 'JPEG', quality=88, optimize=True, progressive=True)
        after = os.path.getsize(f)
        total_before += before
        total_after += after
    print(f'  头像合计 {kb(total_before):.0f}K -> {kb(total_after):.0f}K')


def audit():
    print('\n单文件体检（>200K 需要上 CDN）：')
    big = []
    for f in glob.glob(os.path.join(ROOT, '**', '*'), recursive=True):
        if os.path.isfile(f):
            s = os.path.getsize(f)
            if s > 200 * 1024:
                big.append((f, s))
    if big:
        for f, s in big:
            print(f'  ⚠️ {os.path.relpath(f, ROOT)} {kb(s):.0f}K')
    else:
        print('  ✅ 没有超过 200K 的文件')
    total = sum(os.path.getsize(f) for f in glob.glob(os.path.join(ROOT, '**', '*'), recursive=True) if os.path.isfile(f))
    print(f'  static 总计 {kb(total):.0f}K')


if __name__ == '__main__':
    print('印章：')
    do_stamps()
    print('头像：')
    do_avatars()
    audit()
