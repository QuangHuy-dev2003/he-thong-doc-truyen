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
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = false, exclude = { "roles", "stories", "readingHistory", "favorites", "votes",
        "storyComments", "chapterComments", "wallet", "userVip", "refreshToken" })
@ToString(exclude = { "roles", "stories", "readingHistory", "favorites", "votes", "comments" })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "vip_display_style", length = 100)
    private String vipDisplayStyle;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "first_login", columnDefinition = "boolean default false")
    private Boolean firstLogin = false;

    // Quan hệ với Role - Many-to-Many
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    // Quan hệ với Story - One-to-Many (tác giả)
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Story> stories = new HashSet<>();

    // Quan hệ với ReadingHistory - One-to-Many
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ReadingHistory> readingHistory = new HashSet<>();

    // Quan hệ với Favorite - One-to-Many
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Favorite> favorites = new HashSet<>();

    // Quan hệ với Vote - One-to-Many
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Vote> votes = new HashSet<>();

    // Quan hệ với Comment - One-to-Many
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<StoryComment> storyComments = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ChapterComment> chapterComments = new HashSet<>();

    // Quan hệ với Wallet - One-to-One
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserWallet wallet;

    // Quan hệ với UserVip - One-to-One
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserVip userVip;

    // Quan hệ với RefreshToken - One-to-One
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private RefreshToken refreshToken;
}