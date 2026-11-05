@echo off
REM ===================================================================
REM  启动后端（开发环境 + 本地私有配置）
REM
REM  带 local 的原因：数据库密码、微信 AppSecret 在 application-local.yml 里，
REM  该文件被 .gitignore 忽略、不进版本库。后激活的 profile 优先级更高，
REM  所以 local 会覆盖 dev 里的兜底值。
REM
REM  用法：scripts\start-backend.cmd
REM  可用环境变量覆盖：JAVA_BIN、SERVER_PORT、SPRING_PROFILES
REM ===================================================================

cd /d "%~dp0..\backend"

if "%JAVA_BIN%"=="" set JAVA_BIN=java
if "%SERVER_PORT%"=="" set SERVER_PORT=8080
if "%SPRING_PROFILES%"=="" set SPRING_PROFILES=dev,local

if not exist "ruoyi-admin\target\ruoyi-admin.jar" (
  echo 未找到 ruoyi-admin\target\ruoyi-admin.jar
  echo 请先执行: cd backend ^&^& mvn -pl ruoyi-admin -am package -DskipTests
  exit /b 1
)

echo 启动后端：port=%SERVER_PORT% profiles=%SPRING_PROFILES%
"%JAVA_BIN%" -jar ruoyi-admin\target\ruoyi-admin.jar --server.port=%SERVER_PORT% --spring.profiles.active=%SPRING_PROFILES%
