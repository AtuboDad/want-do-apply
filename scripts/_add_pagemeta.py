"""给用到 OrderSheet 的页面补 <page-meta>，让小程序端弹层打开时页面不许滚。

只改一次的东西，改成幂等：已经有的跳过。
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PAGES = ROOT / "miniapp/src/pages"

TARGETS = {
    "index/index.vue": "sheetVisible",
    "mine/index.vue": "sheetVisible",
    "group/detail.vue": "sheetVisible",
    "notice/index.vue": "sheetVisible",
    "detail/index.vue": "visible",
}

META = """  <!-- #ifdef MP-WEIXIN -->
  <!-- 弹层打开时禁掉页面原生滚动，否则手指在弹层上划，底下的页面会跟着动 -->
  <page-meta :page-style="pageStyle" />
  <!-- #endif -->
"""

for rel, flag in TARGETS.items():
    path = PAGES / rel
    text = path.read_text(encoding="utf-8")
    if "page-meta" in text:
        print(f"skip {rel}")
        continue

    assert text.startswith("<template>\n"), rel
    text = text.replace("<template>\n", "<template>\n" + META, 1)

    # 补 import（各页 vue 的具名导入不完全一样，用正则找）
    import re

    m = re.search(r"^import \{[^}]*\} from 'vue'$", text, re.M)
    assert m, rel
    text = text[: m.end()] + "\nimport { pageStyleWhen } from '../../utils/scroll-lock'" + text[m.end():]

    # 补 computed
    flag_line = f"const {flag} = ref(false)"
    assert flag_line in text, (rel, flag_line)
    text = text.replace(
        flag_line,
        flag_line + f"\n/** 弹层打开时锁页面滚动（仅小程序生效） */\nconst pageStyle = pageStyleWhen(() => {flag}.value)",
        1,
    )

    path.write_text(text, encoding="utf-8")
    print(f"ok {rel}")
