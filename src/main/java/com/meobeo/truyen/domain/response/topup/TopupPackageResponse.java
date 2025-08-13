package com.meobeo.truyen.domain.response.topup;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TopupPackageResponse {
    private Long id;
    private String name;
    private Integer amount;
    private Integer price;
    private Double bonusPercentage;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
