package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "user")
public class RefreshToken {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "token", columnDefinition = "TEXT", unique = true)
    private String token;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    // Quan hệ với User - One-to-One
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
}