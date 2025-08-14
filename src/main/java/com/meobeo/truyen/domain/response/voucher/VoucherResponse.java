package com.meobeo.truyen.domain.response.voucher;

import com.meobeo.truyen.domain.enums.VoucherStatus;
import com.meobeo.truyen.domain.enums.VoucherType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherResponse {
    private Long id;
    private String code;
    private String name;
    private VoucherType type;
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private BigDecimal maxDiscountAmount;
    private VoucherStatus status;
    private Integer maxUsageCount;
    private Integer maxUsersCount;
    private Integer maxUsagePerUser;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String description;
    private LocalDateTime createdAt;
    private Long currentUsageCount;
    private Long currentUsersCount;
}
