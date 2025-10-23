#!/bin/bash

# JWT认证修复验证脚本
# 测试注册和登录API的修复效果

echo "🚀 开始测试JWT认证修复效果..."
echo "=================================="

# 设置API基础URL
BASE_URL="http://localhost:8080"
# 如果前端在运行，也可以使用前端代理地址
# BASE_URL="http://localhost:5173"

echo "📍 API基础URL: $BASE_URL"
echo ""

# 测试1: 用户注册
echo "📝 测试1: 用户注册"
echo "----------------------------------"

REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser'"$(date +%s)"'",
    "password": "123456",
    "userEmail": "test'"$(date +%s)"'@example.com",
    "phoneNumber": "13800138000",
    "sex": 1
  }')

echo "注册响应:"
echo "$REGISTER_RESPONSE" | jq . 2>/dev/null || echo "$REGISTER_RESPONSE"

# 检查注册是否成功
if echo "$REGISTER_RESPONSE" | grep -q '"code":0'; then
    echo "✅ 注册成功！"
elif echo "$REGISTER_RESPONSE" | grep -q "success\|成功"; then
    echo "✅ 注册可能成功（响应格式可能不同）！"
else
    echo "❌ 注册可能失败，请检查响应"
fi

echo ""

# 测试2: 使用已创建的测试用户登录
echo "🔑 测试2: 用户登录"
echo "----------------------------------"

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456"
  }')

echo "登录响应:"
echo "$LOGIN_RESPONSE" | jq . 2>/dev/null || echo "$LOGIN_RESPONSE"

# 提取JWT token（如果登录成功）
JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken // .data.token // .token // empty' 2>/dev/null)

if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
    echo "✅ 登录成功！获取到JWT Token: ${JWT_TOKEN:0:20}..."

    # 测试3: 使用JWT Token访问受保护的API
    echo ""
    echo "🔐 测试3: 使用JWT Token访问受保护API"
    echo "----------------------------------"

    PROTECTED_RESPONSE=$(curl -s -X GET "$BASE_URL/api/articles/userinform?userId=1" \
      -H "Authorization: Bearer $JWT_TOKEN")

    echo "受保护API响应:"
    echo "$PROTECTED_RESPONSE" | jq . 2>/dev/null || echo "$PROTECTED_RESPONSE"

    if echo "$PROTECTED_RESPONSE" | grep -q '"code":0\|success'; then
        echo "✅ JWT认证成功！"
    elif echo "$PROTECTED_RESPONSE" | grep -q "401\|unauthorized\|未授权"; then
        echo "❌ JWT认证失败 - 401 Unauthorized"
    elif echo "$PROTECTED_RESPONSE" | grep -q "用户不存在\|user not found"; then
        echo "⚠️  JWT解析成功，但用户不存在（可能是数据库问题）"
    else
        echo "❓ JWT认证状态未知，请检查响应"
    fi

else
    echo "❌ 登录失败或无法获取JWT Token"

    # 尝试查看响应中的错误信息
    if echo "$LOGIN_RESPONSE" | grep -q "用户名或密码错误\|username or password"; then
        echo "💡 提示: 这可能意味着数据库中没有测试用户，或密码不匹配"
    elif echo "$LOGIN_RESPONSE" | grep -q "连接\|connection\|network"; then
        echo "💡 提示: 这可能是网络连接问题"
    fi
fi

echo ""

# 测试4: 无效Token测试
echo "🚫 测试4: 无效JWT Token测试"
echo "----------------------------------"

INVALID_RESPONSE=$(curl -s -X GET "$BASE_URL/api/articles/userinform?userId=1" \
  -H "Authorization: Bearer invalid.token.here")

echo "无效Token响应:"
echo "$INVALID_RESPONSE" | jq . 2>/dev/null || echo "$INVALID_RESPONSE"

if echo "$INVALID_RESPONSE" | grep -q "401\|unauthorized\|令牌.*无效\|invalid.*token"; then
    echo "✅ 无效Token正确被拒绝"
else
    echo "❓ 无效Token处理状态未知"
fi

echo ""
echo "🏁 测试完成！"
echo "=================================="
echo ""
echo "📋 结果分析:"
echo "1. 如果注册成功，说明 UserMapper.xml 的 insertUser 修复有效"
echo "2. 如果登录成功，说明用户查询和密码验证正常"
echo "3. 如果JWT认证成功，说明 CustomUserDetailsService 和 JwtAuthenticationFilter 修复有效"
echo "4. 如果有任何失败，请检查:"
echo "   - 后端服务是否在 $BASE_URL 运行"
echo "   - 数据库连接是否正常"
echo "   - 是否已执行 test_auth_fix.sql 创建测试用户"
echo ""
echo "💡 下一步:"
echo "1. 执行 SQL 脚本: mysql -u your_user -p your_database < test_auth_fix.sql"
echo "2. 启动后端服务"
echo "3. 重新运行此测试脚本"