package com.meobeo.truyen.service.interfaces;

public interface AsyncEmailService {

    /**
     * Gửi email OTP bất đồng bộ
     * 
     * @param email   Email người nhận
     * @param otpCode Mã OTP
     */
    void sendOtpEmailAsync(String email, String otpCode);

}