package com.meobeo.truyen.domain.response.spiritstone;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SpiritStonePackageResponse {

    private Long id;
    private String name;
    private Integer spiritStones;
    private Integer price;
    private Double bonusPercentage;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
