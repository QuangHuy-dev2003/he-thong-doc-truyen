package com.meobeo.truyen.domain.enums;

public enum TransactionType {
    TOPUP, // Nạp tiền thật (VND)
    SPEND, // Tiêu tiền thật (VND)
    GIFT_CODE, // Nhận linh thạch từ gift code
    SPIRIT_EARN, // Kiếm linh thạch (đọc truyện, hoạt động)
    SPIRIT_SPEND, // Tiêu linh thạch (mua chapter)
    SPIRIT_EXCHANGE, // Đổi linh thạch (VND -> Linh thạch)
    ADMIN_ADJUSTMENT, // Điều chỉnh bởi ADMIN
    RECOMMENDATION_TICKET_EARN, // Kiếm phiếu đề cử
    RECOMMENDATION_TICKET_SPEND, // Tiêu phiếu đề cử
    CHAPTER_UNLOCK_SPIRIT_STONE, // Tiêu linh thạch mở khóa chương
    CHAPTER_UNLOCK_BATCH_SPIRIT_STONE, // Tiêu linh thạch mở khóa nhiều chương
    CHAPTER_UNLOCK_FULL_SPIRIT_STONE // Tiêu linh thạch mở khóa full truyện
}