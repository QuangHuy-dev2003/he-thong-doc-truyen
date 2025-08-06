package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "user_vip_ranking")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = { "user", "vipLevel" })
public class UserVipRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_topup")
    private Integer totalTopup = 0;

    @Column(name = "ranking_month", length = 7)
    private String rankingMonth;

    @Column(name = "rank_position")
    private Integer rankPosition;

    // Quan hệ với User - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Quan hệ với VipLevel - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vip_level_id")
    private VipLevel vipLevel;
}