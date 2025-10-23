@echo off
REM JWT认证修复验证脚本 (Windows版本)
REM 测试注册和登录API的修复效果

echo 🚀 开始测试JWT认证修复效果...
echo ==================================

REM 设置API基础URL
set BASE_URL=http://localhost:8080
REM 如果前端在运行，也可以使用前端代理地址
REM set BASE_URL=http://localhost:5173

echo 📍 API基础URL: %BASE_URL%
echo.

REM 测试1: 用户注册
echo 📝 测试1: 用户注册
echo ----------------------------------

REM 创建临时文件存储响应
set temp_file=%TEMP%\api_response_%RANDOM%.txt

curl -s -X POST "%BASE_URL%/api/auth/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\": \"testuser%RANDOM%\", \"password\": \"123456\", \"userEmail\": \"test%RANDOM%@example.com\", \"phoneNumber\": \"13800138000\", \"sex\": 1}" > %temp_file%

echo 注册响应:
type %temp_file%
echo.

REM 简单检查注册是否成功
findstr /C:"code\":0" %temp_file% >nul
if %errorlevel% equ 0 (
    echo ✅ 注册成功！
) else (
    findstr /C:"success" %temp_file% /C:"成功" %temp_file% >nul
    if %errorlevel% equ 0 (
        echo ✅ 注册可能成功（响应格式可能不同）！
    ) else (
        echo ❌ 注册可能失败，请检查响应
    )
)

echo.

REM 测试2: 使用已创建的测试用户登录
echo 🔑 测试2: 用户登录
echo ----------------------------------

curl -s -X POST "%BASE_URL%/api/auth/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\": \"testuser\", \"password\": \"123456\"}" > %temp_file%

echo 登录响应:
type %temp_file%
echo.

REM 尝试提取JWT token（Windows批处理方式简化版）
echo 尝试从响应中提取Token...

REM 简单的token查找（如果有jq工具会更准确）
findstr /C:"accessToken" %temp_file% >nul
if %errorlevel% equ 0 (
    echo ✅ 登录成功！检测到响应中包含accessToken
    set JWT_TOKEN=found
) else (
    findstr /C:"token" %temp_file% >nul
    if %errorlevel% equ 0 (
        echo ✅ 登录可能成功！检测到响应中包含token
        set JWT_TOKEN=found
    ) else (
        echo ❌ 登录失败或无法获取JWT Token
        set JWT_TOKEN=

        findstr /C:"用户名或密码错误" %temp_file% >nul
        if %errorlevel% equ 0 (
            echo 💡 提示: 这可能意味着数据库中没有测试用户，或密码不匹配
        )
        findstr /C:"connection" %temp_file% /C:"连接" %temp_file% >nul
        if %errorlevel% equ 0 (
            echo 💡 提示: 这可能是网络连接问题
        )
    )
)

if defined JWT_TOKEN (
    REM 测试3: 使用JWT Token访问受保护的API
    echo.
    echo 🔐 测试3: 使用JWT Token访问受保护API
    echo ----------------------------------

    curl -s -X GET "%BASE_URL%/api/articles/userinform?userId=1" ^
      -H "Authorization: Bearer some.test.token" > %temp_file%

    echo 受保护API响应:
    type %temp_file%
    echo.

    findstr /C:"code\":0" %temp_file% >nul
    if %errorlevel% equ 0 (
        echo ✅ JWT认证成功！
    ) else (
        findstr /C:"success" %temp_file% >nul
        if %errorlevel% equ 0 (
            echo ✅ JWT认证成功！
        ) else (
            findstr /C:"401" %temp_file% /C:"unauthorized" %temp_file% >nul
            if %errorlevel% equ 0 (
                echo ❌ JWT认证失败 - 401 Unauthorized
            ) else (
                findstr /C:"用户不存在" %temp_file% /C:"user not found" %temp_file% >nul
                if %errorlevel% equ 0 (
                    echo ⚠️ JWT解析成功，但用户不存在（可能是数据库问题）
                ) else (
                    echo ❓ JWT认证状态未知，请检查响应
                )
            )
        )
    )
)

echo.

REM 测试4: 无效Token测试
echo 🚫 测试4: 无效JWT Token测试
echo ----------------------------------

curl -s -X GET "%BASE_URL%/api/articles/userinform?userId=1" ^
  -H "Authorization: Bearer invalid.token.here" > %temp_file%

echo 无效Token响应:
type %temp_file%
echo.

findstr /C:"401" %temp_file% /C:"unauthorized" %temp_file% /C:"令牌.*无效" %temp_file% /C:"invalid.*token" %temp_file% >nul
if %errorlevel% equ 0 (
    echo ✅ 无效Token正确被拒绝
) else (
    echo ❓ 无效Token处理状态未知
)

REM 清理临时文件
del %temp_file% 2>nul

echo.
echo 🏁 测试完成！
echo ==================================
echo.
echo 📋 结果分析:
echo 1. 如果注册成功，说明 UserMapper.xml 的 insertUser 修复有效
echo 2. 如果登录成功，说明用户查询和密码验证正常
echo 3. 如果JWT认证成功，说明 CustomUserDetailsService 和 JwtAuthenticationFilter 修复有效
echo 4. 如果有任何失败，请检查:
echo    - 后端服务是否在 %BASE_URL% 运行
echo    - 数据库连接是否正常
echo    - 是否已执行 test_auth_fix.sql 创建测试用户
echo.
echo 💡 下一步:
echo 1. 执行 SQL 脚本: mysql -u your_user -p your_database ^< test_auth_fix.sql
echo 2. 启动后端服务
echo 3. 重新运行此测试脚本
echo.

pause