package com.meobeo.truyen.domain.response.giftcode;

import com.meobeo.truyen.domain.enums.GiftCodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiftCodeResponse {

    private Long id;
    private String code;
    private String name;
    private Integer amount;
    private GiftCodeType type;
    private Integer maxUsageCount;
    private Integer maxUsersCount;
    private Integer maxUsagePerUser;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private Long totalUsageCount;
    private Long uniqueUserCount;
}
