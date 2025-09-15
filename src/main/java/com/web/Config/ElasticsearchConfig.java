package com.web.Config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.apache.http.HttpHost;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;

@Configuration
@ConditionalOnProperty(
    value = "elasticsearch.enabled",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
public class ElasticsearchConfig {

    // 使用系统属性或环境变量作为后备，优先级：系统属性 > 环境变量 > 硬编码默认值
    private String caPath = resolveCaPath();
    private String esUri = resolveEsUri();
    private String esUsername = resolveEsUsername();
    private String esPassword = resolveEsPassword();

    /**
     * 解析证书路径，支持多级后备
     */
    private String resolveCaPath() {
        // 1. 系统属性优先
        String path = System.getProperty("elasticsearch.http-ca-path");
        if (path != null && !path.trim().isEmpty()) {
            return path;
        }

        // 2. 环境变量
        path = System.getenv("ES_HTTP_CA_PATH");
        if (path != null && !path.trim().isEmpty()) {
            return path;
        }

        // 3. 硬编码默认值
        return "classpath:es/http_ca.crt";
    }

    /**
     * 解析ES URI
     */
    private String resolveEsUri() {
        String uri = System.getProperty("elasticsearch.uris");
        if (uri != null && !uri.trim().isEmpty()) {
            return uri;
        }

        uri = System.getenv("ES_URIS");
        if (uri != null && !uri.trim().isEmpty()) {
            return uri;
        }

        return "https://localhost:9200";
    }

    /**
     * 解析ES用户名
     */
    private String resolveEsUsername() {
        String username = System.getProperty("elasticsearch.username");
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }

        username = System.getenv("ES_USERNAME");
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }

        return "elastic";
    }

    /**
     * 解析ES密码
     */
    private String resolveEsPassword() {
        String password = System.getProperty("elasticsearch.password");
        if (password != null && !password.trim().isEmpty()) {
            return password;
        }

        password = System.getenv("ES_PASSWORD");
        if (password != null && !password.trim().isEmpty()) {
            return password;
        }

        return "123456";
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
        value = "elasticsearch.enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public ElasticsearchClient elasticsearchClient() throws Exception {
        // 强制使用HTTPS
        URI uri = new URI(esUri);
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Elasticsearch必须使用HTTPS连接，当前配置: " + scheme);
        }

        String host = uri.getHost() == null ? "localhost" : uri.getHost();
        int port = uri.getPort() == -1 ? 9200 : uri.getPort();

        // 验证用户名和密码是否配置
        if (esUsername == null || esUsername.isEmpty()) {
            throw new IllegalArgumentException("Elasticsearch用户名未配置");
        }
        if (esPassword == null || esPassword.isEmpty()) {
            throw new IllegalArgumentException("Elasticsearch密码未配置");
        }

        // 设置认证
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(host, port),
                new UsernamePasswordCredentials(esUsername, esPassword)
        );

        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme));

        // 强制加载SSL证书
        InputStream in = null;
        try {
            if (caPath.startsWith("classpath:")) {
                String r = caPath.substring("classpath:".length());
                in = getClass().getClassLoader().getResourceAsStream(r);
                if (in == null) {
                    throw new IllegalStateException("无法在classpath中找到SSL证书: " + r +
                        "\n请确保证书文件存在于: src/main/resources/es/http_ca.crt");
                }
            } else if (caPath.startsWith("file:")) {
                in = new FileInputStream(caPath.substring("file:".length()));
            } else {
                in = new FileInputStream(caPath);
            }

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate ca = cf.generateCertificate(in);

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("es-http-ca", ca);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder
                    .setSSLContext(sslContext)
                    .setDefaultCredentialsProvider(credsProvider)
            );

            log.info("Elasticsearch SSL配置成功，使用证书: {}", caPath);
        } catch (Exception e) {
            log.error("Elasticsearch SSL证书配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法配置Elasticsearch SSL证书: " + e.getMessage(), e);
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
        }

        RestClient restClient = builder.build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
