package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vip_levels")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = { "userVips", "vipDiscounts" })
public class VipLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "effect_description", columnDefinition = "TEXT")
    private String effectDescription;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Quan hệ với UserVip - One-to-Many
    @OneToMany(mappedBy = "vipLevel", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<UserVip> userVips = new HashSet<>();

    // Quan hệ với VipChapterDiscount - One-to-Many
    @OneToMany(mappedBy = "vipLevel", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<VipChapterDiscount> vipDiscounts = new HashSet<>();
}