package com.web.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * SSH服务器配置类
 * 提供SSH终端功能的基础配置
 *
 * 注意：完整的SSH服务器实现需要额外的SSH依赖库
 * 此配置类提供了SSH功能的配置框架
 */
@Slf4j
@Configuration
@Profile("ssh")
@ConditionalOnProperty(name = "ssh.server.enabled", havingValue = "true")
public class SshServerConfig {

    @Value("${ssh.server.port:2222}")
    private int sshPort;

    @Value("${ssh.server.host:0.0.0.0}")
    private String sshHost;

    @Value("${ssh.server.banner:Welcome to WEEB SSH Terminal!}")
    private String sshBanner;

    @Value("${ssh.server.max-sessions:10}")
    private int maxSessions;

    /**
     * SSH服务器配置信息
     *
     * SSH功能已准备就绪，包含以下组件：
     * - CommandManager: 命令管理器
     * - InteractionConnect: SSH连接处理器
     * - Custom Commands: 多个自定义命令
     *
     * 要启用SSH服务器，需要：
     * 1. 添加SSH依赖库到pom.xml
     * 2. 取消注释application-ssh.properties中的配置
     * 3. 实现完整的SSH服务器启动逻辑
     */
    public void printSshConfiguration() {
        log.info("=== SSH服务器配置 ===");
        log.info("端口: {}", sshPort);
        log.info("主机: {}", sshHost);
        log.info("Banner: {}", sshBanner);
        log.info("最大会话数: {}", maxSessions);
        log.info("状态: 配置就绪，需要完整的SSH服务器实现");
        log.info("==================");
    }
}