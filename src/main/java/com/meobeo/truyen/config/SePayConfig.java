package com.meobeo.truyen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sepay")
public class SePayConfig {

    /**
     * Số tài khoản ngân hàng
     */
    private String accountNumber = "0362600321";

    /**
     * Tên ngân hàng
     */
    private String bankName = "VPBank";

    /**
     * URL tạo QR code
     */
    private String qrUrl = "https://qr.sepay.vn/img";

    /**
     * Thời gian timeout cho yêu cầu nạp tiền (phút)
     */
    private int timeoutMinutes = 30;

    /**
     * Secret key để verify webhook (nếu cần)
     */
    private String webhookSecret;

    /**
     * Tạo URL QR code hoàn chỉnh
     */
    public String buildQrUrl(String amount, String description) {
        return String.format("%s?acc=%s&bank=%s&amount=%s&des=%s",
                qrUrl, accountNumber, bankName, amount, description);
    }
}
