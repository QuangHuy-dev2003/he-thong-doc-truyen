package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "chapter_payments")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = { "chapter", "story" })
public class ChapterPayment {

    @Id
    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "is_vip_only")
    private Boolean isVipOnly = false;

    @Column(name = "is_locked")
    private Boolean isLocked = false;

    @Column(name = "story_id", nullable = false)
    private Long storyId;

    // Quan hệ với Chapter - One-to-One
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    // Quan hệ với Story - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false, insertable = false, updatable = false)
    private Story story;
}