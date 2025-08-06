package com.meobeo.truyen.service.interfaces;

public interface OtpService {

    String generateOtp();

    void createAndSendOtp(String email, Long userId);

    boolean verifyOtp(String email, String otpCode);

    void markOtpAsUsed(String email, String otpCode);
}