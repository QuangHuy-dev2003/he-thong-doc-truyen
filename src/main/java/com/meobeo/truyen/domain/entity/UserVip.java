package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_vips")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = { "user", "vipLevel" })
public class UserVip {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "start_date")
    @CreationTimestamp
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Quan hệ với User - One-to-One
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // Quan hệ với VipLevel - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vip_level_id")
    private VipLevel vipLevel;
}