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

    /**
     * Gửi email thông báo nạp tiền thành công bất đồng bộ
     * 
     * @param email       Email người nhận
     * @param userName    Tên người dùng
     * @param packageName Tên gói nạp tiền
     * @param amount      Số tiền nạp
     * @param newBalance  Số dư mới
     * @param time        Thời gian giao dịch
     * @param walletUrl   URL kiểm tra ví
     */
    void sendTopupSuccessEmailAsync(String email, String userName, String packageName,
            String amount, String newBalance, String time, String walletUrl);

}