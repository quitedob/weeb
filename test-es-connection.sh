#!/bin/bash
# WEEB Elasticsearch连接测试脚本
# 用于测试ES服务连接是否正常

echo "========================================"
echo "WEEB Elasticsearch连接测试"
echo "========================================"

# 检查Elasticsearch服务状态
echo "[1/3] 检查Elasticsearch服务..."
if curl -s -X GET "https://172.18.48.1:9200/_cluster/health" --cacert "src/main/resources/es/http_ca.crt" -u "elastic:$ES_PASSWORD" > /dev/null 2>&1; then
    echo "✅ HTTPS连接成功"
elif curl -s -X GET "http://172.18.48.1:9200/_cluster/health" > /dev/null 2>&1; then
    echo "✅ HTTP连接成功（建议配置HTTPS）"
else
    echo "❌ 连接失败"
    echo "请检查："
    echo "1. Elasticsearch服务是否启动"
    echo "2. 端口是否正确（9200）"
    echo "3. 如果使用HTTPS，证书路径是否正确"
    echo "4. ES_PASSWORD环境变量是否设置"
    exit 1
fi

# 检查集群状态
echo ""
echo "[2/3] 获取集群状态..."
if curl -s -X GET "https://172.18.48.1:9200/_cluster/health?pretty" --cacert "src/main/resources/es/http_ca.crt" -u "elastic:$ES_PASSWORD" 2>/dev/null; then
    :
elif curl -s -X GET "http://172.18.48.1:9200/_cluster/health?pretty" 2>/dev/null; then
    :
fi

# 检查索引
echo ""
echo "[3/3] 检查现有索引..."
if curl -s -X GET "https://172.18.48.1:9200/_cat/indices?v" --cacert "src/main/resources/es/http_ca.crt" -u "elastic:$ES_PASSWORD" 2>/dev/null; then
    :
elif curl -s -X GET "http://172.18.48.1:9200/_cat/indices?v" 2>/dev/null; then
    :
fi

echo ""
echo "========================================"
echo "测试完成！"
echo "========================================"
echo "如果连接成功，可以启动Spring Boot应用："
echo "./start.sh"
echo "========================================"
