#!/usr/bin/env bash
#
# 启动后端（开发环境 + 本地私有配置）
#
# 为什么要带 local：数据库密码、微信 AppSecret 放在 application-local.yml 里，
# 该文件被 .gitignore 忽略、不进版本库。Spring Boot 中后激活的 profile 优先级更高，
# 所以 local 会覆盖 dev 里的兜底值。
#
# 用法：
#   ./scripts/start-backend.sh              # 默认 8080
#   SERVER_PORT=9090 ./scripts/start-backend.sh
#   JAVA_BIN=/d/Java/jdk-21.0.6/bin/java ./scripts/start-backend.sh
#
set -e

cd "$(dirname "$0")/../backend"

JAR="ruoyi-admin/target/ruoyi-admin.jar"
if [ ! -f "$JAR" ]; then
  echo "未找到 $JAR"
  echo "请先执行: cd backend && mvn -pl ruoyi-admin -am package -DskipTests"
  exit 1
fi

JAVA_BIN="${JAVA_BIN:-java}"
SERVER_PORT="${SERVER_PORT:-8080}"
PROFILES="${SPRING_PROFILES:-dev,local}"

case "$("$JAVA_BIN" -version 2>&1 | head -1)" in
  *'"1.8'*) echo "⚠️  当前 JAVA_BIN 是 JDK 8，本项目需要 JDK 21+"; echo "   用 JAVA_BIN=/path/to/jdk-21/bin/java 再试"; exit 1;;
esac

echo "启动后端：port=$SERVER_PORT profiles=$PROFILES"
exec "$JAVA_BIN" -jar "$JAR" --server.port="$SERVER_PORT" --spring.profiles.active="$PROFILES"
