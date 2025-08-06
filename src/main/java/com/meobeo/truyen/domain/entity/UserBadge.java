package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_badges")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "userAssignments")
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    // Quan hệ với UserBadgeAssignment - One-to-Many
    @OneToMany(mappedBy = "badge", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<UserBadgeAssignment> userAssignments = new HashSet<>();
}