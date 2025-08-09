package com.meobeo.truyen.domain.entity;

import com.meobeo.truyen.domain.enums.StoryStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "stories")
@Data
@EqualsAndHashCode(exclude = { "genres", "chapters", "favorites", "votes", "comments", "chapterComments" })
@ToString(exclude = { "genres", "chapters", "favorites", "votes", "comments", "chapterComments" })
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "slug", unique = true, nullable = false)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "author_name")
    private String authorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StoryStatus status = StoryStatus.ONGOING;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Quan hệ với User (tác giả) - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    // Quan hệ với Chapter - One-to-Many
    @OneToMany(mappedBy = "story", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Chapter> chapters = new HashSet<>();

    // Quan hệ với Genre - Many-to-Many
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "story_genres", joinColumns = @JoinColumn(name = "story_id"), inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    // Quan hệ với Favorite - One-to-Many
    @OneToMany(mappedBy = "story", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Favorite> favorites = new HashSet<>();

    // Quan hệ với Vote - One-to-Many
    @OneToMany(mappedBy = "story", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Vote> votes = new HashSet<>();

    // Quan hệ với StoryComment - One-to-Many
    @OneToMany(mappedBy = "story", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<StoryComment> comments = new HashSet<>();

    @OneToMany(mappedBy = "story", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ChapterComment> chapterComments = new HashSet<>();
}