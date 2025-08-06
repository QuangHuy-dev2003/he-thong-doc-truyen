package com.meobeo.truyen.service.interfaces;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otpCode);

}