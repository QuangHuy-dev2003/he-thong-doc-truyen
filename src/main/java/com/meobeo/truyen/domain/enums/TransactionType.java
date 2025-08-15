package com.meobeo.truyen.domain.enums;

public enum TransactionType {
    TOPUP, // Nạp tiền thật (VND)
    SPEND, // Tiêu tiền thật (VND)
    GIFT_CODE, // Nhận linh thạch từ gift code
    SPIRIT_EARN, // Kiếm linh thạch (đọc truyện, hoạt động)
    SPIRIT_SPEND, // Tiêu linh thạch (mua chapter)
    SPIRIT_EXCHANGE // Đổi linh thạch (VND -> Linh thạch)
}