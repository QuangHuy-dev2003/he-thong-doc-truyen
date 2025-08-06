package com.meobeo.truyen.domain.entity;

import com.meobeo.truyen.domain.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "author_requests")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "user")
public class AuthorRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với User - Many-to-One
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}