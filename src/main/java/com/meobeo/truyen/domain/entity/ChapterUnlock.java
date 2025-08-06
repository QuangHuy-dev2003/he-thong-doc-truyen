package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_unlocks")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = { "user", "chapter" })
public class ChapterUnlock {

    @EmbeddedId
    private ChapterUnlockId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chapterId")
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @CreationTimestamp
    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;
}