package com.meobeo.truyen.domain.response.giftcode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiftCodeUsageResponse {

    private String giftCodeName;
    private Integer amountReceived;
    private String message;
    private Boolean success;
}
