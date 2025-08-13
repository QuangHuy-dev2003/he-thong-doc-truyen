package com.meobeo.truyen.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiftCodeUsageId implements Serializable {

    @Column(name = "gift_code_id")
    private Long giftCodeId;

    @Column(name = "user_id")
    private Long userId;
}
