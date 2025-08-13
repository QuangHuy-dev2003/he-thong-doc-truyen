package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity lưu session view để chống spam
 * Mỗi IP/session chỉ được tăng view 1 lần trong khoảng thời gian nhất định
 */
@Entity
@Table(name = "story_view_sessions")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "story")
public class StoryViewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "story_id", nullable = false)
    private Long storyId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", insertable = false, updatable = false)
    private Story story;
}
