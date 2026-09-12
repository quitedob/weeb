package com.web.service.impl;

import com.web.service.PasswordResetDeliveryService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SmtpPasswordResetDeliveryService implements PasswordResetDeliveryService {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String frontendUrl;
    private final String from;

    public SmtpPasswordResetDeliveryService(ObjectProvider<JavaMailSender> mailSender,
            @Value("${password-reset.frontend-url:http://localhost:5173/reset-password}") String frontendUrl,
            @Value("${password-reset.from:}") String from) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.from = from;
    }

    @Override public boolean isAvailable() { return mailSender.getIfAvailable() != null && !from.isBlank(); }

    @Override
    public void send(String email, String token) {
        if (!isAvailable()) throw new IllegalStateException("Password reset email is not configured");
        String link = UriComponentsBuilder.fromUriString(frontendUrl).queryParam("token", token).build().toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("WEEB 密码重置");
        message.setText("请在15分钟内打开以下链接重置密码。链接只能使用一次。\n" + link);
        mailSender.getObject().send(message);
    }
}
