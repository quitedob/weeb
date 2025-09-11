#!/bin/bash

# Weeb 项目部署脚本
# 使用方法: ./deploy.sh [dev|prod|stop|restart]

set -e

PROJECT_NAME="weeb"
COMPOSE_FILE="docker-compose.yml"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查Docker和Docker Compose是否安装
check_dependencies() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi

    log_info "依赖检查通过"
}

# 检查环境变量文件
check_env_file() {
    if [ ! -f ".env" ]; then
        log_warn ".env 文件不存在，正在从示例文件创建..."
        if [ -f "config-example.properties" ]; then
            cp config-example.properties .env
            log_warn "已创建 .env 文件，请编辑其中的配置项"
            exit 1
        else
            log_error "config-example.properties 文件不存在"
            exit 1
        fi
    fi

    log_info "环境配置文件检查通过"
}

# 启动服务
start_services() {
    log_info "正在启动 Weeb 服务..."

    # 停止可能存在的旧服务
    docker-compose -f $COMPOSE_FILE down 2>/dev/null || true

    # 构建并启动服务
    docker-compose -f $COMPOSE_FILE up -d --build

    log_info "服务启动中，请稍候..."

    # 等待服务启动完成
    sleep 30

    # 检查服务状态
    check_services_status

    log_info "Weeb 服务启动成功！"
    log_info "前端地址: http://localhost"
    log_info "后端API: http://localhost:8080"
}

# 停止服务
stop_services() {
    log_info "正在停止 Weeb 服务..."
    docker-compose -f $COMPOSE_FILE down
    log_info "Weeb 服务已停止"
}

# 重启服务
restart_services() {
    log_info "正在重启 Weeb 服务..."
    docker-compose -f $COMPOSE_FILE restart
    log_info "Weeb 服务重启完成"
}

# 检查服务状态
check_services_status() {
    log_info "检查服务状态..."

    # 检查各个服务是否启动成功
    services=("mysql" "redis" "elasticsearch" "weeb-app")

    for service in "${services[@]}"; do
        if docker-compose -f $COMPOSE_FILE ps $service | grep -q "Up"; then
            log_info "$service 服务运行正常"
        else
            log_error "$service 服务启动失败"
            docker-compose -f $COMPOSE_FILE logs $service
            exit 1
        fi
    done

    log_info "所有服务运行正常"
}

# 查看日志
show_logs() {
    service=${2:-"weeb-app"}
    log_info "显示 $service 服务日志..."
    docker-compose -f $COMPOSE_FILE logs -f $service
}

# 清理Docker资源
cleanup() {
    log_info "清理Docker资源..."
    docker-compose -f $COMPOSE_FILE down -v --rmi all
    docker system prune -f
    log_info "清理完成"
}

# 备份数据
backup_data() {
    log_info "备份数据..."

    backup_dir="./backup/$(date +%Y%m%d_%H%M%S)"
    mkdir -p $backup_dir

    # 备份MySQL数据
    docker exec weeb-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD:-root} weeb > $backup_dir/weeb.sql

    # 备份配置文件
    cp .env $backup_dir/ 2>/dev/null || true
    cp docker-compose.yml $backup_dir/ 2>/dev/null || true

    log_info "数据备份完成，备份文件位于: $backup_dir"
}

# 主函数
main() {
    case "${1:-start}" in
        "start")
            check_dependencies
            check_env_file
            start_services
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            restart_services
            ;;
        "status")
            check_services_status
            ;;
        "logs")
            show_logs "$@"
            ;;
        "cleanup")
            cleanup
            ;;
        "backup")
            backup_data
            ;;
        "dev")
            log_info "开发模式启动..."
            export SPRING_PROFILES_ACTIVE=dev
            start_services
            ;;
        "prod")
            log_info "生产模式启动..."
            export SPRING_PROFILES_ACTIVE=prod
            start_services
            ;;
        *)
            echo "使用方法: $0 [start|stop|restart|status|logs|cleanup|backup|dev|prod]"
            echo ""
            echo "命令说明:"
            echo "  start   - 启动所有服务"
            echo "  stop    - 停止所有服务"
            echo "  restart - 重启所有服务"
            echo "  status  - 检查服务状态"
            echo "  logs    - 查看服务日志 (可选指定服务名)"
            echo "  cleanup - 清理Docker资源"
            echo "  backup  - 备份数据"
            echo "  dev     - 开发模式启动"
            echo "  prod    - 生产模式启动"
            exit 1
            ;;
    esac
}

main "$@"

