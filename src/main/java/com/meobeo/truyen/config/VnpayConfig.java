package com.meobeo.truyen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vnp")
public class VnpayConfig {

    /**
     * Mã website tại VNPAY
     */
    private String TmnCode;

    /**
     * Chuỗi bí mật
     */
    private String HashSecret;

    /**
     * URL thanh toán VNPAY
     */
    private String Url;

    /**
     * URL callback sau khi thanh toán
     */
    private String ReturnUrl;

    /**
     * URL kiểm tra trạng thái giao dịch
     */
    private String TransactionQueryUrl;

    /**
     * Phiên bản API (mặc định)
     */
    private String version = "2.1.0";

    /**
     * Lệnh thanh toán (mặc định)
     */
    private String command = "pay";

    /**
     * Loại tiền tệ (mặc định)
     */
    private String currency = "VND";

    /**
     * Ngôn ngữ (mặc định)
     */
    private String locale = "vn";

    /**
     * Loại hàng hóa (mặc định)
     */
    private String orderType = "topup";

    /**
     * Thời gian timeout giao dịch (phút)
     */
    private int timeoutMinutes = 15;

    // Getter methods để tương thích với code hiện tại
    public String getTmnCode() {
        return TmnCode;
    }

    public String getHashSecret() {
        return HashSecret;
    }

    public String getUrl() {
        return Url;
    }

    public String getReturnUrl() {
        return ReturnUrl;
    }

    public String getTransactionQueryUrl() {
        return TransactionQueryUrl;
    }
}
