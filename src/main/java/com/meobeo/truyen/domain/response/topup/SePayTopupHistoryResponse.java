package com.meobeo.truyen.domain.response.topup;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SePayTopupHistoryResponse {

    private List<SePayTopupHistoryItem> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    @Data
    public static class SePayTopupHistoryItem {
        private Long requestId;
        private BigDecimal amount;
        private BigDecimal originalAmount;
        private BigDecimal discountAmount;
        private String voucherCode;
        private String transferContent;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime processedAt;
    }
}
