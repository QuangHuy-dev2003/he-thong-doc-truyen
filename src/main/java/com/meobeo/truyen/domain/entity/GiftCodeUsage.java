package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "gift_code_usages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiftCodeUsage {

    @EmbeddedId
    private GiftCodeUsageId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("giftCodeId")
    @JoinColumn(name = "gift_code_id")
    private GiftCode giftCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;
}
