@echo off
REM WEEB Elasticsearch连接测试脚本
REM 用于测试ES服务连接是否正常

echo ========================================
echo WEEB Elasticsearch连接测试
echo ========================================

REM 检查Elasticsearch服务状态
echo [1/3] 检查Elasticsearch服务...
curl -s -X GET "https://172.18.48.1:9200/_cluster/health" --cacert "src/main/resources/es/http_ca.crt" -u "elastic:%ES_PASSWORD%" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ HTTPS连接成功
) else (
    echo ⚠️ HTTPS连接失败，尝试HTTP连接...
    curl -s -X GET "http://172.18.48.1:9200/_cluster/health" >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ HTTP连接成功（建议配置HTTPS）
    ) else (
        echo ❌ 连接失败
        echo 请检查：
        echo 1. Elasticsearch服务是否启动
        echo 2. 端口是否正确（9200）
        echo 3. 如果使用HTTPS，证书路径是否正确
        pause
        exit /b 1
    )
)

REM 检查集群状态
echo.
echo [2/3] 获取集群状态...
curl -s -X GET "https://172.18.48.1:9200/_cluster/health?pretty" --cacert "src/main/resources/es/http_ca.crt" -u "elastic:%ES_PASSWORD%" 2>nul
if %errorlevel% neq 0 (
    curl -s -X GET "http://172.18.48.1:9200/_cluster/health?pretty" 2>nul
)

REM 检查索引
echo.
echo [3/3] 检查现有索引...
curl -s -X GET "https://172.18.48.1:9200/_cat/indices?v" --cacert "src/main/resources/es/http_ca.crt" -u "elastic:%ES_PASSWORD%" 2>nul
if %errorlevel% neq 0 (
    curl -s -X GET "http://172.18.48.1:9200/_cat/indices?v" 2>nul
)

echo.
echo ========================================
echo 测试完成！
echo ========================================
echo 如果连接成功，可以启动Spring Boot应用：
echo start.bat
echo ========================================

pause
