@echo off
REM WEEB 项目快速启动脚本 (Windows)
REM 用法: start.bat [dev|prod]

echo 🚀 WEEB 项目启动脚本
echo =========================

REM 检查Java环境
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java 未安装，请先安装 JDK 17+
    pause
    exit /b 1
)

REM 检查Maven环境
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Maven 未安装，请先安装 Maven 3.6+
    pause
    exit /b 1
)

REM 检查MySQL连接
echo 🔍 检查 MySQL 连接...
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ⚠️  MySQL 客户端未安装，将跳过连接检查
) else (
    REM 尝试连接MySQL（使用默认配置）
    mysql -hlocalhost -P3306 -uroot -proot -e "SELECT 1;" >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ MySQL 连接正常
    ) else (
        echo ⚠️  MySQL 连接失败，请检查 MySQL 服务是否启动
        echo    或者修改 application.yml 中的数据库配置
    )
)

REM 设置启动模式
set PROFILE=dev
if "%1"=="prod" (
    set PROFILE=prod
    echo 🔧 使用生产环境配置
) else (
    echo 🔧 使用开发环境配置
)

REM 编译项目
echo 🔨 编译项目...
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo ❌ 编译失败，请检查代码
    pause
    exit /b 1
)

echo ✅ 编译成功

REM 启动应用
echo 🚀 启动 WEEB 应用...
echo 📊 启动模式: %PROFILE%
echo 🌐 应用将在 http://localhost:8080 启动
echo 📝 日志输出:
echo.

REM 设置JVM参数
set JVM_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

REM 启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=%PROFILE% -Dspring-boot.run.jvmArguments="%JVM_OPTS%"

echo.
echo 👋 WEEB 应用已停止
pause
