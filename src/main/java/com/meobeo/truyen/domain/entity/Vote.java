package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = { "user", "story" })
public class Vote {

    @EmbeddedId
    private VoteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("storyId")
    @JoinColumn(name = "story_id")
    private Story story;

    @CreationTimestamp
    @Column(name = "voted_at")
    private LocalDateTime votedAt;
}