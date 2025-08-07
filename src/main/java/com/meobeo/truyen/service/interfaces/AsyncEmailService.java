package com.meobeo.truyen.service.interfaces;

public interface AsyncEmailService {

    /**
     * Gửi email OTP bất đồng bộ
     * 
     * @param email   Email người nhận
     * @param otpCode Mã OTP
     */
    void sendOtpEmailAsync(String email, String otpCode);

    /**
     * Gửi email quên mật khẩu bất đồng bộ
     * 
     * @param email   Email người nhận
     * @param otpCode Mã OTP
     */
    void sendForgotPasswordEmailAsync(String email, String otpCode);

}