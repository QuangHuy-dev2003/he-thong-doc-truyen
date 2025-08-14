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
public class VoucherUsageId implements Serializable {

    @Column(name = "voucher_id")
    private Long voucherId;

    @Column(name = "user_id")
    private Long userId;
}
