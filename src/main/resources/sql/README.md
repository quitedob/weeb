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
**总计: 28张表**

#### 用户管理模块 (3张)
- `01_create_user_table.sql` - 用户基础信息表
- `02_create_user_stats_table.sql` - 用户统计数据表
- `19_create_user_level_history_table.sql` - 用户等级历史表

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

#### 文章内容模块 (8张)
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

#### 系统管理模块 (4张)
- `09_create_notification_table.sql` - 通知表
- `18_create_system_log_table.sql` - 系统日志表
- `21_create_content_report_table.sql` - 内容举报表

### 数据插入脚本 (insert/)
- `01_insert_default_users.sql` - 默认用户数据
- `02_insert_article_categories.sql` - 文章分类数据
- `03_insert_article_tags.sql` - 文章标签数据

### 索引优化脚本 (index/)
- `01_optimize_core_indexes.sql` - 核心表索引优化
- `02_optimize_content_indexes.sql` - 内容相关表索引优化


## 使用说明

1. **开发环境**: DatabaseInitializer.java 会自动按顺序执行所有 SQL 文件
2. **生产环境**: 需要手动执行 SQL 文件或使用数据库迁移工具
3. **修改规范**: 所有数据库结构变更都应该通过修改对应的 SQL 文件实现

## 执行顺序

1. 检查数据库连接
2. 创建表结构
3. 插入初始数据
4. 优化数据库索引

## 注意事项

- 所有 SQL 文件都使用 `IF NOT EXISTS` 语法，避免重复创建
- 索引优化脚本使用 `IF NOT EXISTS` 语法，避免重复创建索引
- 创建脚本已包含所有必要的字段和索引，无需额外的迁移步骤

---
创建时间: 2025-11-10
更新说明: 从 DatabaseInitializer.java 提取并重构 SQL 文件，集成迁移逻辑到创建脚本