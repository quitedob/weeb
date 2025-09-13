# Elasticsearch SSL证书配置

## 🔐 获取Elasticsearch CA证书

由于Elasticsearch 9.x默认启用TLS，您需要配置SSL证书才能连接。

### 方法1：从Elasticsearch安装目录复制

如果您的Elasticsearch安装在本地，请从以下位置复制CA证书：

```bash
# Windows (IntelliJ IDEA集成的Elasticsearch)
# 从Elasticsearch配置目录复制
copy "D:\IntelliJ IDEA 2023.3.8\utils\elasticsearch-9.1.3\config\certs\http_ca.crt" "D:\java\weeb\src\main\resources\es\http_ca.crt"

# Linux/Mac
# cp /path/to/elasticsearch/config/certs/http_ca.crt src/main/resources/es/http_ca.crt
```

### 方法2：通过Elasticsearch API下载

如果无法直接访问文件系统，可以通过API下载：

```bash
# 下载CA证书
curl -X GET "https://localhost:9200/_ssl/certificates" \
  -H "Authorization: Basic <base64编码的用户名:密码>" \
  --cacert /path/to/elasticsearch/config/certs/http_ca.crt \
  -o ca_cert.pem
```

### 方法3：临时禁用TLS（开发环境）

如果无法获取证书，可以临时禁用Elasticsearch的TLS：

#### 步骤1：修改Elasticsearch配置
编辑 `elasticsearch.yml`（通常在config目录下）：

```yaml
# 临时禁用TLS（仅用于开发环境）
xpack.security.http.ssl.enabled: false
xpack.security.transport.ssl.enabled: false
```

#### 步骤2：重启Elasticsearch
```bash
# Windows
elasticsearch.bat restart

# Linux/Mac
./bin/elasticsearch -d
```

#### 步骤3：修改应用配置
将 `application.yml` 中的ES配置改为HTTP：

```yaml
elasticsearch:
  enabled: true
  uris: http://172.18.48.1:9200  # 改为HTTP
  connection-timeout: 10000ms
  socket-timeout: 60000ms
  # 移除username, password, ssl配置
```

#### ⚠️ 安全警告
**此方法仅适用于开发环境！**
生产环境必须启用TLS和认证，否则会有严重的安全风险。

### 方法4：使用环境变量配置证书路径

如果证书在不同位置，可以使用环境变量：

```bash
# 设置证书路径
ES_CERT_PATH=/path/to/your/http_ca.crt
```

然后在`application.yml`中：

```yaml
ssl:
  bundle:
    pem:
      es-pem:
        truststore:
          certificate: ${ES_CERT_PATH}
```

## 🔧 配置说明

### 快速选择方案

#### 方案A：使用HTTPS + SSL证书（推荐，安全）
1. **获取证书**：复制 `http_ca.crt` 到 `src/main/resources/es/`
2. **设置环境变量**：
   ```bash
   # Windows
   set ES_PASSWORD=your_elasticsearch_password

   # Linux/Mac
   export ES_PASSWORD=your_elasticsearch_password
   ```
3. **使用当前配置**：无需修改，应用会自动使用HTTPS

#### 方案B：使用HTTP连接（临时，开发环境）
如果不想配置证书，修改 `application.yml`：

```yaml
elasticsearch:
  enabled: true
  uris: http://172.18.48.1:9200  # 改为HTTP
  connection-timeout: 10000ms
  socket-timeout: 60000ms
  # 注释掉或删除以下配置
  # username: elastic
  # password: ${ES_PASSWORD:}
  # restclient:
  #   ssl:
  #     bundle: es-pem
```

### 验证连接

#### HTTPS连接测试：
```bash
curl -X GET "https://172.18.48.1:9200/_cluster/health" \
  -u "elastic:$ES_PASSWORD" \
  --cacert src/main/resources/es/http_ca.crt
```

#### HTTP连接测试：
```bash
curl -X GET "http://172.18.48.1:9200/_cluster/health"
```

## ⚠️ 安全注意事项

1. **不要将证书提交到版本控制系统**
   在`.gitignore`中添加：
   ```
   src/main/resources/es/http_ca.crt
   ```

2. **密码管理**
   - 使用环境变量存储密码
   - 不要在代码中硬编码密码
   - 生产环境使用密钥管理服务

3. **证书更新**
   - Elasticsearch重启时可能重新生成证书
   - 证书变更后需要更新应用配置

## 🔍 故障排除

### 连接失败
```
javax.net.ssl.SSLHandshakeException: PKIX path building failed
```
**解决方案**: 检查证书路径和文件名是否正确

### 认证失败
```
org.elasticsearch.ElasticsearchStatusException: Unable to authenticate user
```
**解决方案**: 检查用户名和密码是否正确

### 证书验证失败
```
javax.net.ssl.SSLPeerUnverifiedException: Certificate for <localhost> doesn't match
```
**解决方案**: 检查证书中的主机名是否匹配
