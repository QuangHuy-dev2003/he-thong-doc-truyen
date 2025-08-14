package com.meobeo.truyen.service.interfaces;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otpCode);

    void sendForgotPasswordEmail(String toEmail, String otpCode);

    void sendTopupSuccessEmail(String toEmail, String userName, String packageName,
            String amount, String newBalance, String time, String walletUrl);

}