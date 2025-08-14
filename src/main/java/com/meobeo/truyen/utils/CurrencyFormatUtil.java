package com.meobeo.truyen.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CurrencyFormatUtil {

    private static final Locale VIETNAM_LOCALE = new Locale("vi", "VN");
    private static final NumberFormat VIETNAM_CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(VIETNAM_LOCALE);
    private static final DecimalFormat VIETNAM_NUMBER_FORMAT = new DecimalFormat("#,###");

    /**
     * Format số tiền theo chuẩn Việt Nam với đơn vị VNĐ
     * Ví dụ: 30000 -> "30,000 VNĐ"
     */
    public static String formatVNDCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VNĐ";
        }

        try {
            // Format số với dấu phẩy ngăn cách hàng nghìn
            String formattedNumber = VIETNAM_NUMBER_FORMAT.format(amount.longValue());
            return formattedNumber + " VNĐ";
        } catch (Exception e) {
            log.warn("Lỗi format tiền tệ: {}", e.getMessage());
            return amount.toString() + " VNĐ";
        }
    }

    /**
     * Format số tiền theo chuẩn Việt Nam với đơn vị VNĐ (từ Integer)
     * Ví dụ: 30000 -> "30,000 VNĐ"
     */
    public static String formatVNDCurrency(Integer amount) {
        if (amount == null) {
            return "0 VNĐ";
        }
        return formatVNDCurrency(BigDecimal.valueOf(amount));
    }

    /**
     * Format số tiền theo chuẩn Việt Nam với đơn vị VNĐ (từ Long)
     * Ví dụ: 30000L -> "30,000 VNĐ"
     */
    public static String formatVNDCurrency(Long amount) {
        if (amount == null) {
            return "0 VNĐ";
        }
        return formatVNDCurrency(BigDecimal.valueOf(amount));
    }

    /**
     * Format số tiền theo chuẩn Việt Nam với đơn vị VNĐ (từ String)
     * Ví dụ: "30000" -> "30,000 VNĐ"
     */
    public static String formatVNDCurrency(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return "0 VNĐ";
        }

        try {
            BigDecimal bigDecimal = new BigDecimal(amount);
            return formatVNDCurrency(bigDecimal);
        } catch (NumberFormatException e) {
            log.warn("Không thể parse số tiền: {}", amount);
            return amount + " VNĐ";
        }
    }

    /**
     * Format số tiền theo chuẩn Việt Nam (chỉ số, không có đơn vị)
     * Ví dụ: 30000 -> "30,000"
     */
    public static String formatVNDNumber(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }

        try {
            return VIETNAM_NUMBER_FORMAT.format(amount.longValue());
        } catch (Exception e) {
            log.warn("Lỗi format số: {}", e.getMessage());
            return amount.toString();
        }
    }

    /**
     * Format số tiền theo chuẩn Việt Nam (chỉ số, không có đơn vị) từ Integer
     * Ví dụ: 30000 -> "30,000"
     */
    public static String formatVNDNumber(Integer amount) {
        if (amount == null) {
            return "0";
        }
        return formatVNDNumber(BigDecimal.valueOf(amount));
    }
}