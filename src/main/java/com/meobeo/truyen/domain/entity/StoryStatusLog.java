package com.meobeo.truyen.domain.entity;

import com.meobeo.truyen.domain.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "story_status_logs")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = { "story", "changedBy" })
public class StoryStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RequestStatus status;

    @CreationTimestamp
    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    // Quan hệ với Story - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id")
    private Story story;

    // Quan hệ với User (người thay đổi) - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;
}