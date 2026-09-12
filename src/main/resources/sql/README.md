# WEEB 项目 SQL 文件目录

本目录包含 WEEB 项目的所有 SQL 文件，按功能分类组织。

## 目录结构

```
sql/
├── check/          # 检查脚本
├── create/         # 表创建脚本
├── insert/         # 数据插入脚本
├── index/          # 索引优化脚本
├── migration/      # 数据迁移脚本
└── README.md       # 本文件
```

## 文件说明

### 检查脚本 (check/)
- `01_check_database_connection.sql` - 数据库连接检查
- `02_check_table_structure.sql` - 表结构检查

### 表创建脚本 (create/)
**基线30张业务表；V003新增2张认证表，V004新增1张outbox表，加迁移记录共34张表。** 当前注册表由 `schema-manifest.json` 定义。

#### 用户管理模块 (5张)
- `01_create_user_table.sql` - 用户基础信息表
- `02_create_user_stats_table.sql` - 用户统计数据表
- `19_create_user_level_history_table.sql` - 用户等级历史表
- `28_create_user_preferences_table.sql` - 用户隐私与通知偏好；各项默认开启，分区更新互不覆盖
- `29_create_user_login_day_table.sql` - 每个用户每天最多记录一次成功登录，用于累计登录天数

#### 聊天系统模块 (6张)
- `04_create_shared_chat_table.sql` - 共享聊天表
- `05_create_chat_list_table.sql` - 聊天列表表
- `06_create_message_table.sql` - 消息内容表
- `06_5_create_chat_unread_count_table.sql` - 聊天未读计数表
- `26_create_message_retry_table.sql` - 消息重试表
- `27_create_message_reaction_table.sql` - 消息反应表

#### 群组管理模块 (4张)
- `03_create_group_table.sql` - 群组信息表
- `07_create_group_member_table.sql` - 群组成员表
- `23_create_group_transfer_history_table.sql` - 群组转让历史表
- `24_create_group_application_table.sql` - 群组申请表

#### 文章内容模块 (9张)
- `08_create_article_table.sql` - 文章内容表
- `11_create_article_comment_table.sql` - 文章评论表
- `12_create_article_like_table.sql` - 文章点赞表
- `13_create_article_favorite_table.sql` - 文章收藏表
- `14_create_article_category_table.sql` - 文章分类表
- `15_create_article_tag_table.sql` - 文章标签表
- `16_create_article_tag_relation_table.sql` - 文章与标签关联表
- `20_create_article_version_table.sql` - 文章版本表
- `25_create_article_moderation_history_table.sql` - 文章审核历史表

#### 社交关系模块 (3张)
- `10_create_contact_table.sql` - 联系人表
- `17_create_user_follow_table.sql` - 用户关注表
- `22_create_contact_group_table.sql` - 联系人分组表

#### 系统管理模块 (3张)
- `09_create_notification_table.sql` - 通知表
- `18_create_system_log_table.sql` - 系统日志表
- `21_create_content_report_table.sql` - 内容举报表

### 数据插入脚本 (insert/)
- `01_insert_default_users.sql` - 安全管理员配置说明（不创建默认账号或密码）
- `02_insert_article_categories.sql` - 文章分类数据
- `03_insert_article_tags.sql` - 文章标签数据

### 索引优化脚本 (index/)
- `01_optimize_core_indexes.sql` - 核心表索引优化
- `02_optimize_content_indexes.sql` - 内容相关表索引优化


## 使用说明

1. **开发环境**: `DatabaseInitializer` 按 `schema-manifest.json` 执行唯一的版本化迁移清单。
2. **生产环境**: 启动只读校验；先用独立 `SchemaMigrationCli` 查看 `--plan`，核查备份后 `--apply`，再 `--validate`，详见[迁移说明](../../../../../docs/schema-migrations.md)。
3. **修改规范**: 已应用版本与基线建表文件不再修改；新增后续迁移版本。失败保留记录，MySQL DDL可能已部分提交，人工核查后才允许同校验和的 `--apply --resume-failed`。

## 执行顺序

1. 从配置的 MySQL URL 确定数据库，确保数据库存在并检查连接
2. 创建表结构
3. 插入初始数据；分类使用 `04_insert_article_categories_by_name.sql` 按名称解析父分类，保留旧库非标准ID
4. 查询数据库元数据，仅创建尚不存在的同名索引
5. 执行连接和表结构检查脚本，再依次应用V002历史兼容、V003认证持久化、V004消息可靠性迁移

## 注意事项

- 建表脚本使用 `CREATE TABLE IF NOT EXISTS`；检查、初始数据和迁移脚本应按各自语义执行
- 手工执行索引脚本前应检查已有索引，避免同名索引重复创建
- 初始化器通过数据库元数据跳过已存在的索引；其余 SQL 执行失败会中止初始化
- 现有数据库不会因 CREATE TABLE IF NOT EXISTS 自动更新列；必须执行注册表中的迁移，旧 `db/migration` 原型和旧分类种子不作为第二套活动入口
- 消息线索后端引用的 `message_threads` 和 `thread_participants` 不在上述 30 张表内，当前没有对应建表脚本；全新部署不支持线索功能

---
创建时间: 2025-11-10
建表执行清单以 `src/main/java/com/web/config/DatabaseInitializer.java` 为准；迁移脚本单独部署。
## 安全迁移

`migration/01_secure_user_roles.sql` 规范既有 user.type 并设为 NOT NULL。先备份并审核现有管理员；脚本不会根据用户名授予权限。新账号始终为 USER，管理员需通过授权运维流程按已验证的用户 ID 设置。生产环境不自动运行迁移。
