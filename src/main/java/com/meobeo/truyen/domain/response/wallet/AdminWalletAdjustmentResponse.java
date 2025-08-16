package com.meobeo.truyen.domain.response.wallet;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminWalletAdjustmentResponse {

    private Long userId;
    private String username;
    private String displayName;
    private Integer oldBalance;
    private Integer newBalance;
    private Integer oldSpiritStones;
    private Integer newSpiritStones;
    private Integer oldRecommendationTickets;
    private Integer newRecommendationTickets;
    private Integer adjustedAmount;
    private String currency;
    private String adjustmentType;
    private String description;
    private LocalDateTime adjustedAt;
    private String adjustedBy; // Tên admin thực hiện
}
