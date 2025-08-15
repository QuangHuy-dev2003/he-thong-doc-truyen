package com.meobeo.truyen.domain.response.spiritstone;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExchangeHistoryResponse {

    private Long id;
    private Integer amount;
    private String currency; // VND hoặc SPIRIT_STONE
    private String type; // SPIRIT_EXCHANGE hoặc SPIRIT_EARN
    private String description;
    private LocalDateTime createdAt;
}
