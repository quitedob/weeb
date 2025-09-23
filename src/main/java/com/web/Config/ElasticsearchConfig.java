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

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUri;

    @Value("${spring.elasticsearch.username:}")
    private String esUsername;

    @Value("${spring.elasticsearch.password:}")
    private String esPassword;

    @Value("${elasticsearch.ca-path:classpath:es/http_ca.crt}")
    private String caPath;


    @Bean
    @Primary
    @ConditionalOnProperty(
        value = "elasticsearch.enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public ElasticsearchClient elasticsearchClient() throws Exception {
        URI uri = new URI(esUri);
        String scheme = uri.getScheme();
        String host = uri.getHost() == null ? "localhost" : uri.getHost();
        int port = uri.getPort() == -1 ? 9200 : uri.getPort();

        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme));

        // 如果使用 HTTPS，需要 SSL 和认证
        if ("https".equalsIgnoreCase(scheme)) {
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

            // 加载SSL证书
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
        } else {
            // HTTP 模式，不需要认证和SSL
            log.info("Elasticsearch使用HTTP连接，无需SSL和认证");
        }

        RestClient restClient = builder.build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
