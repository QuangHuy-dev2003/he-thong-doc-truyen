package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "revenue_sharing")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = { "chapter", "payer", "author" })
public class RevenueSharing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "admin_share", nullable = false)
    private Integer adminShare;

    @Column(name = "author_share", nullable = false)
    private Integer authorShare;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Quan hệ với Chapter - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    // Quan hệ với User (người mua) - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    // Quan hệ với User (tác giả) - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
}