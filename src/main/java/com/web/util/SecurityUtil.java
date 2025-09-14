package com.web.util;

import com.web.exception.WeebException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public final class SecurityUtil {

    private static final KeyPair keyPair;
    private static final BCryptPasswordEncoder passwordEncoder;
    // AES密钥从环境变量读取，避免硬编码
    private static final String AES_SECRET_KEY;
    // RSA密钥对存储路径
    private static final String KEYSTORE_DIR = System.getProperty("user.dir") + "/keystore";
    private static final String PUBLIC_KEY_FILE = KEYSTORE_DIR + "/rsa_public.key";
    private static final String PRIVATE_KEY_FILE = KEYSTORE_DIR + "/rsa_private.key";

    static {
        try {
            // 从环境变量读取AES密钥，如果未设置则使用默认值（仅开发环境）
            AES_SECRET_KEY = System.getProperty("AES_SECRET_KEY",
                    System.getenv().getOrDefault("AES_SECRET_KEY", "WeebWeebWeebWeebserver2025"));

            // 验证AES密钥安全性
            validateAesKey();

            // 加载或生成RSA密钥对
            keyPair = loadOrGenerateKeyPair();
            passwordEncoder = new BCryptPasswordEncoder();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("安全工具初始化失败: " + e.getMessage());
        }
    }

    /**
     * 加载已存在的RSA密钥对，如果不存在则生成新的并保存
     */
    private static KeyPair loadOrGenerateKeyPair() throws Exception {
        // 检查密钥文件是否存在
        Path publicKeyPath = Paths.get(PUBLIC_KEY_FILE);
        Path privateKeyPath = Paths.get(PRIVATE_KEY_FILE);

        if (Files.exists(publicKeyPath) && Files.exists(privateKeyPath)) {
            try {
                log.info("加载已存在的RSA密钥对...");
                // 从文件加载密钥对
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");

                // 读取公钥
                byte[] publicKeyBytes = Files.readAllBytes(publicKeyPath);
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
                PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

                // 读取私钥
                byte[] privateKeyBytes = Files.readAllBytes(privateKeyPath);
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
                PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

                return new KeyPair(publicKey, privateKey);
            } catch (Exception e) {
                log.warn("加载RSA密钥对失败，将生成新的密钥对: {}", e.getMessage());
            }
        }

        // 生成新的密钥对并保存
        log.info("生成新的RSA密钥对...");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair newKeyPair = keyGen.generateKeyPair();

        // 保存到文件
        try {
            Files.createDirectories(Paths.get(KEYSTORE_DIR));

            // 保存公钥
            Files.write(publicKeyPath, newKeyPair.getPublic().getEncoded());

            // 保存私钥
            Files.write(privateKeyPath, newKeyPair.getPrivate().getEncoded());

            log.info("RSA密钥对已保存到: {}", KEYSTORE_DIR);
        } catch (IOException e) {
            log.warn("保存RSA密钥对失败，重启后将重新生成: {}", e.getMessage());
        }

        return newKeyPair;
    }

    /**
     * 验证AES密钥的安全性
     */
    private static void validateAesKey() {
        if (AES_SECRET_KEY == null || AES_SECRET_KEY.trim().isEmpty()) {
            throw new ExceptionInInitializerError("AES密钥不能为空，请设置环境变量AES_SECRET_KEY");
        }

        if (AES_SECRET_KEY.length() < 16) {
            throw new ExceptionInInitializerError("AES密钥长度不足，至少需要16个字符");
        }

        // 检查是否使用默认值
        String defaultKey = "WeebWeebWeebWeebserver2025";
        if (defaultKey.equals(AES_SECRET_KEY)) {
            String warningMsg = "警告: 检测到使用默认AES密钥，这是不安全的！请设置环境变量AES_SECRET_KEY";
            System.err.println(warningMsg);
            if (isProductionEnvironment()) {
                throw new ExceptionInInitializerError("生产环境禁止使用默认AES密钥");
            }
        }
    }

    /**
     * 检查是否为生产环境
     */
    private static boolean isProductionEnvironment() {
        String activeProfile = System.getProperty("spring.profiles.active", "");
        String envProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        return "prod".equals(activeProfile) || "production".equals(activeProfile) ||
               "prod".equals(envProfile) || "production".equals(envProfile);
    }

    public static String getPublicKey() {
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyBytes = publicKey.getEncoded();
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(publicKeyBytes)
                + "\n-----END PUBLIC KEY-----";
    }

    public static String decryptPassword(String encryptedPassword) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new WeebException("密码解析失败~");
        }
    }

    public static String encryptPassword(String password) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new WeebException("密码加密失败~");
        }
    }

    public static boolean verifyPassword(String password, String passwordHash) {
        return passwordEncoder.matches(password, passwordHash);
    }

    public static String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    private static SecretKeySpec getSecretAesKeySpec() {
        if (AES_SECRET_KEY == null || AES_SECRET_KEY.length() < 16) {
            throw new IllegalStateException("AES 密钥无效，请在环境变量中设置 AES_SECRET_KEY");
        }
        return new SecretKeySpec(AES_SECRET_KEY.substring(0, 16).getBytes(StandardCharsets.UTF_8), "AES");
    }

    public static String aesEncrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, getSecretAesKeySpec());
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES 加密失败：", e);
            throw new WeebException("生成失败~");
        }
    }

    public static String aesDecrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, getSecretAesKeySpec());
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES 解密失败：", e);
            throw new WeebException("解析失败~");
        }
    }
}
