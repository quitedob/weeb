#!/bin/bash

# WEEB 项目快速启动脚本
# 用法: ./start.sh [dev|prod]

echo "🚀 WEEB 项目启动脚本"
echo "========================"

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "❌ Java 未安装，请先安装 JDK 17+"
    exit 1
fi

# 检查Maven环境
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven 未安装，请先安装 Maven 3.6+"
    exit 1
fi

# 检查MySQL连接
echo "🔍 检查 MySQL 连接..."
if ! command -v mysql &> /dev/null; then
    echo "⚠️  MySQL 客户端未安装，将跳过连接检查"
else
    # 尝试连接MySQL（使用默认配置）
    if mysql -hlocalhost -P3306 -uroot -proot -e "SELECT 1;" &> /dev/null; then
        echo "✅ MySQL 连接正常"
    else
        echo "⚠️  MySQL 连接失败，请检查 MySQL 服务是否启动"
        echo "   或者修改 application.yml 中的数据库配置"
    fi
fi

# 设置启动模式
PROFILE="dev"
if [ "$1" = "prod" ]; then
    PROFILE="prod"
    echo "🔧 使用生产环境配置"
else
    echo "🔧 使用开发环境配置"
fi

# 编译项目
echo "🔨 编译项目..."
if ! mvn clean compile -q; then
    echo "❌ 编译失败，请检查代码"
    exit 1
fi

echo "✅ 编译成功"

# 启动应用
echo "🚀 启动 WEEB 应用..."
echo "📊 启动模式: $PROFILE"
echo "🌐 应用将在 http://localhost:8080 启动"
echo "📝 日志输出:"

# 设置JVM参数
JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE -Dspring-boot.run.jvmArguments="$JVM_OPTS"

echo "👋 WEEB 应用已停止"
