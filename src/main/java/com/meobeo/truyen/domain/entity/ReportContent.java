package com.meobeo.truyen.domain.entity;

import com.meobeo.truyen.domain.enums.ReportTargetType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_contents")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "reporter")
public class ReportContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private ReportTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Quan hệ với User (người báo cáo) - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;
}