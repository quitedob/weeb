package com.web.service;

public interface PasswordResetDeliveryService {
    boolean isAvailable();
    void send(String email, String token);
}
