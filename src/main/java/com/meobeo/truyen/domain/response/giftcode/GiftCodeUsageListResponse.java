package com.meobeo.truyen.domain.response.giftcode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiftCodeUsageListResponse {

    private List<GiftCodeUsageDetailResponse> usages;
    private int totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GiftCodeUsageDetailResponse {
        private Long giftCodeId;
        private String giftCodeName;
        private String giftCodeCode;
        private Long userId;
        private String username;
        private String email;
        private Integer amountReceived;
        private LocalDateTime usedAt;
    }
}
