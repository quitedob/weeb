package com.web.util;

import com.web.exception.WeebException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;

@Slf4j
public final class SecurityUtil {

    private static final KeyPair keyPair;
    private static final BCryptPasswordEncoder passwordEncoder;
    // 静态常量：AES密钥，字符串形式
    private static final String AES_SECRET_KEY = "WeebWeebWeebWeebserver2025";
    static {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048); // 改为 2048 位密钥
            keyPair = keyGen.generateKeyPair();
            passwordEncoder = new BCryptPasswordEncoder();
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError("RSA 密钥初始化失败: " + e.getMessage());
        }
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
