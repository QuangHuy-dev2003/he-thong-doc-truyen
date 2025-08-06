package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "user_wallets")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "user")
public class UserWallet {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "balance")
    private Integer balance = 0;

    // Quan hệ với User - One-to-One
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
}