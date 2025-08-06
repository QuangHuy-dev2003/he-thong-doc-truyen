package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "revenue_config")
@Data
@EqualsAndHashCode(callSuper = false)
public class RevenueConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_share_percent")
    private Integer authorSharePercent = 80;

    @Column(name = "admin_share_percent")
    private Integer adminSharePercent = 20;
}