package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_otp")
@Data
@EqualsAndHashCode(callSuper = false)
public class EmailOtp {

    @Id
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "otp_code", length = 10)
    private String otpCode;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}