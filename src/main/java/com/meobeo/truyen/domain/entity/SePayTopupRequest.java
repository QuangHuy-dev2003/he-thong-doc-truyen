package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sepay_topup_requests")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "user")
public class SePayTopupRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "original_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "transfer_content", nullable = false, length = 100)
    private String transferContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TopupStatus status = TopupStatus.PENDING;

    @Column(name = "sepay_transaction_id", length = 100)
    private String sepayTransactionId;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với User - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    public enum TopupStatus {
        PENDING, // Chờ thanh toán
        SUCCESS, // Thanh toán thành công
        FAILED, // Thanh toán thất bại
        EXPIRED // Hết hạn
    }
}
