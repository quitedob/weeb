# Elasticsearch 配置指南

## 📋 当前配置状态

应用已配置为使用 **HTTP 模式** 连接 Elasticsearch，无需 SSL 证书和认证。

## 🔧 基本配置

### 1. 禁用 Elasticsearch 安全认证

找到 Elasticsearch 配置文件 `elasticsearch.yml`，添加或修改以下配置：

```yaml
# 禁用安全认证（开发环境）
xpack.security.enabled: false
```

### 2. 重启 Elasticsearch 服务

```bash
# Windows
.\bin\elasticsearch.bat restart

# Linux/Mac
./bin/elasticsearch -d
```

### 3. 安装 IK 中文分词器（可选）

为了获得更好的中文搜索体验，建议安装 IK 分词器：

```bash
# 下载 IK 分词器（根据 ES 版本选择对应的版本）
# Windows
.\bin\elasticsearch-plugin.bat install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.18.6/elasticsearch-analysis-ik-8.18.6.zip

# Linux/Mac
./bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.18.6/elasticsearch-analysis-ik-8.18.6.zip

# 如果本地有插件文件
# Windows
.\bin\elasticsearch-plugin.bat install file:///D:/path/to/elasticsearch-analysis-ik-8.18.6.zip

# Linux/Mac
./bin/elasticsearch-plugin install file:///path/to/elasticsearch-analysis-ik-8.18.6.zip
```

## ✅ 验证配置

### 测试 HTTP 连接
```bash
curl -X GET "http://localhost:9200/_cluster/health"
```

### 测试集群状态
```bash
curl -X GET "http://localhost:9200/_cat/nodes?v"
```

### 测试索引创建
```bash
curl -X PUT "http://localhost:9200/test-index" -H 'Content-Type: application/json' -d '{"mappings": {"properties": {"content": {"type": "text"}}}}'
```

### 应用启动测试
应用启动后会显示以下日志：
```
INFO  Elasticsearch使用HTTP连接，无需SSL和认证
INFO  数据库连接成功
INFO  数据库表检查完成
INFO  用户表创建成功
INFO  文章表创建成功
...
INFO  Started WeebApplication in X.XXX seconds
```

## 📊 应用中的配置

### Spring Boot 配置
应用已配置在 `application.yml` 中：

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    enabled: true
```

### Elasticsearch 文档模型
消息文档使用标准文本字段：

```java
@Field(type = FieldType.Text)
private String content;
```

## 🔍 搜索功能使用

### API 接口
- `GET /api/search/messages?q=关键词` - 搜索消息
- `GET /api/search/users?q=用户名` - 搜索用户

### 搜索示例
```bash
# 搜索包含"hello"的消息
curl "http://localhost:8080/api/search/messages?q=hello"

# 搜索用户
curl "http://localhost:8080/api/search/users?q=张三"
```

## ⚠️ 注意事项

1. **开发环境安全**：当前配置禁用 ES 安全认证，仅适用于开发环境
2. **生产环境**：生产环境必须启用 ES 安全认证
3. **IK 分词器**：安装 IK 分词器后，重启 ES 服务生效
4. **索引重建**：修改分词器配置后，可能需要重建索引

## 🚀 故障排除

### 连接超时
- 检查 Elasticsearch 服务是否启动
- 确认端口 9200 未被占用
- 验证防火墙设置

### 搜索无结果
- 确认 IK 分词器已正确安装
- 检查索引是否已创建
- 验证文档是否已正确索引

### 性能问题
- 监控 ES 内存和 CPU 使用情况
- 根据需要调整 JVM 参数
- 考虑使用集群模式
