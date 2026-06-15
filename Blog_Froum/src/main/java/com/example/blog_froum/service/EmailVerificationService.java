package com.example.blog_froum.service;

public interface EmailVerificationService {

    void sendRegistrationCode(String email);

    void verifyRegistrationCode(String email, String code);

    void sendEmailChangeCode(Long userId, String newEmail);

    void verifyEmailChangeCode(String newEmail, String code);

    void sendPasswordChangeCode(Long userId);

    void verifyPasswordChangeCode(Long userId, String code);

    void sendPasswordResetCode(String account);

    void resetPassword(String account, String code, String newPassword);
}
