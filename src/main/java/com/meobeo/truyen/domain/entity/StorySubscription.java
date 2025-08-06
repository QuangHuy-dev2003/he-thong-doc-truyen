package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "story_subscriptions")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = { "user", "story" })
public class StorySubscription {

    @EmbeddedId
    private StorySubscriptionId id;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "subscribed_at")
    private LocalDateTime subscribedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("storyId")
    @JoinColumn(name = "story_id")
    private Story story;
}