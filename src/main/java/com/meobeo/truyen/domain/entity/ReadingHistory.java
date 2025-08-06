package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reading_history")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = { "user", "chapter" })
public class ReadingHistory {

    @EmbeddedId
    private ReadingHistoryId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chapterId")
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @CreationTimestamp
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;
}