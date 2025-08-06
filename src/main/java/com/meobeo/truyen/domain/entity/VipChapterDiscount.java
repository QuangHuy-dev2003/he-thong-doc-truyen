package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "vip_chapter_discounts")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = { "vipLevel", "chapter" })
public class VipChapterDiscount {

    @EmbeddedId
    private VipChapterDiscountId id;

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("vipLevelId")
    @JoinColumn(name = "vip_level_id")
    private VipLevel vipLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chapterId")
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;
}