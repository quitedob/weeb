package com.web.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * SQL文件加载工具类
 * 用于从resources/sql目录加载SQL文件内容
 */
@Slf4j
@Component
public class SqlFileLoader {

    /**
     * 从classpath加载SQL文件内容
     * @param sqlFilePath SQL文件路径，相对于sql目录
     * @return SQL文件内容
     */
    public String loadSqlFile(String sqlFilePath) {
        try {
            ClassPathResource resource = new ClassPathResource("sql/" + sqlFilePath);

            if (!resource.exists()) {
                log.error("SQL文件不存在: {}", sqlFilePath);
                throw new RuntimeException("SQL文件不存在: " + sqlFilePath);
            }

            StringBuilder content = new StringBuilder();
            try (Scanner scanner = new Scanner(resource.getInputStream(), StandardCharsets.UTF_8.name())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    // 跳过注释行
                    if (!line.trim().startsWith("--")) {
                        content.append(line).append("\n");
                    }
                }
            }

            String sqlContent = content.toString();
            log.debug("成功加载SQL文件: {}, 内容长度: {}", sqlFilePath, sqlContent.length());
            return sqlContent;

        } catch (IOException e) {
            log.error("加载SQL文件失败: {}", sqlFilePath, e);
            throw new RuntimeException("加载SQL文件失败: " + sqlFilePath, e);
        }
    }

    /**
     * 检查SQL文件是否存在
     * @param sqlFilePath SQL文件路径
     * @return 文件是否存在
     */
    public boolean sqlFileExists(String sqlFilePath) {
        try {
            ClassPathResource resource = new ClassPathResource("sql/" + sqlFilePath);
            return resource.exists();
        } catch (Exception e) {
            log.warn("检查SQL文件存在性时出错: {}", sqlFilePath, e);
            return false;
        }
    }
}