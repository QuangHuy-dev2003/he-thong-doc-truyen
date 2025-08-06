package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_badge_assignments")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = { "user", "badge" })
public class UserBadgeAssignment {

    @EmbeddedId
    private UserBadgeAssignmentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("badgeId")
    @JoinColumn(name = "badge_id")
    private UserBadge badge;

    @CreationTimestamp
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
}