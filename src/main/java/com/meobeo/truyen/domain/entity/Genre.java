package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "genres")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "stories")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false, length = 50)
    private String name;

    // Quan hệ với Story - Many-to-Many
    @ManyToMany(mappedBy = "genres", fetch = FetchType.LAZY)
    private Set<Story> stories = new HashSet<>();
}