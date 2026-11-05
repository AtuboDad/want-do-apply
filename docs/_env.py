"""读取 docs/.env 并注入环境变量（不依赖第三方库）

`.env` 不进 Git（见根目录 .gitignore），里面放本地/测试环境的连接信息：

    WDA_DB_HOST=127.0.0.1
    WDA_DB_PORT=3306
    WDA_DB_USER=root
    WDA_DB_PWD=your-password
    WDA_DB_NAME=want_do_apply
    WDA_REDIS_HOST=127.0.0.1
    WDA_REDIS_PORT=6379

没有 .env 也没关系，直接用当前 shell 的环境变量即可。
"""
import os

ENV_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env")


def load():
    if not os.path.exists(ENV_FILE):
        return
    with open(ENV_FILE, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            # shell 里已设置的优先，避免覆盖真实环境
            os.environ.setdefault(key, value)


def get(key, default=None):
    """取配置；没有就返回默认值"""
    return os.environ.get(key, default)


def mysql_conf():
    """MySQL 连接参数（密码没配置时直接报错，避免带着空密码瞎试）"""
    pwd = os.environ.get("WDA_DB_PWD")
    if pwd is None:
        raise SystemExit(
            "缺少数据库密码：请设置环境变量 WDA_DB_PWD，或在 docs/.env 里写一行 WDA_DB_PWD=..."
        )
    return dict(
        host=os.environ.get("WDA_DB_HOST", "127.0.0.1"),
        port=int(os.environ.get("WDA_DB_PORT", "3306")),
        user=os.environ.get("WDA_DB_USER", "root"),
        password=pwd,
        database=os.environ.get("WDA_DB_NAME", "want_do_apply"),
        charset="utf8mb4",
    )


def redis_conf():
    return (
        os.environ.get("WDA_REDIS_HOST", "127.0.0.1"),
        int(os.environ.get("WDA_REDIS_PORT", "6379")),
    )


load()
