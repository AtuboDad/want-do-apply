"""临时开关管理端登录验证码（仅供本地冒烟测试）

用法:
  python _toggle_captcha.py off   # 关闭（跑 _smoke_admin.py 前）
  python _toggle_captcha.py on    # 恢复（跑完记得开回来）
  python _toggle_captcha.py       # 查看当前值

注意：验证码实际由 application.yml 的 captcha.enable 控制，
改库只影响走 sys_config 的逻辑，最稳的方式是启动时传环境变量：
  CAPTCHA_ENABLE=false
"""
import sys

import pymysql

import _env

KEY = "sys.account.captchaEnabled"

conf = _env.mysql_conf()
conf.pop("database", None)
conf["database"] = conf.get("database") or "want_do_apply"

conn = pymysql.connect(**conf)
try:
    with conn.cursor() as cur:
        arg = (sys.argv[1] if len(sys.argv) > 1 else "").lower()
        if arg in ("off", "on"):
            cur.execute(
                "insert into sys_config (config_key, config_value) values (%s, %s) "
                "on duplicate key update config_value = values(config_value)",
                (KEY, "false" if arg == "off" else "true"),
            )
            conn.commit()
        cur.execute("select config_value from sys_config where config_key=%s", (KEY,))
        row = cur.fetchone()
        print(f"{KEY} = {row[0] if row else '(未配置)'}")
        print("提示：验证码实际由 captcha.enable 控制，推荐启动时用 CAPTCHA_ENABLE=false")
finally:
    conn.close()
