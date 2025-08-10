package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chapters", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "story_id", "chapter_number" })
}, indexes = {
        @Index(name = "idx_chapters_story_chapter", columnList = "story_id, chapter_number"),
        @Index(name = "idx_chapters_slug", columnList = "slug")
})
@Data
@EqualsAndHashCode(exclude = { "story", "unlocks", "readingHistory", "comments", "payment", "vipDiscounts" })
@ToString(exclude = { "story", "unlocks", "readingHistory", "comments", "payment", "vipDiscounts" })
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(name = "slug", unique = true, nullable = false, length = 500)
    private String slug;

    @Column(name = "title", length = 1000)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Quan hệ với Story - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    // Quan hệ với ChapterUnlock - One-to-Many
    @OneToMany(mappedBy = "chapter", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ChapterUnlock> unlocks = new HashSet<>();

    // Quan hệ với ReadingHistory - One-to-Many
    @OneToMany(mappedBy = "chapter", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ReadingHistory> readingHistory = new HashSet<>();

    // Quan hệ với ChapterComment - One-to-Many
    @OneToMany(mappedBy = "chapter", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ChapterComment> comments = new HashSet<>();

    // Quan hệ với ChapterPayment - One-to-One
    @OneToOne(mappedBy = "chapter", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private ChapterPayment payment;

    // Quan hệ với VipChapterDiscount - One-to-Many
    @OneToMany(mappedBy = "chapter", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<VipChapterDiscount> vipDiscounts = new HashSet<>();
}