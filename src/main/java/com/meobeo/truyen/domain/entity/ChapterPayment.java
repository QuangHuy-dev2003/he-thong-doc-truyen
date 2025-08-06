package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "chapter_payments")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "chapter")
public class ChapterPayment {

    @Id
    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "is_vip_only")
    private Boolean isVipOnly = false;

    // Quan hệ với Chapter - One-to-One
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;
}