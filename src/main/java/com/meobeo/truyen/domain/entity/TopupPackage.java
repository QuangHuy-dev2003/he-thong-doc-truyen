package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "topup_packages")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class TopupPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên gói không được để trống")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Mệnh giá không được để trống")
    @Min(value = 1, message = "Mệnh giá phải lớn hơn 0")
    @Column(name = "amount", nullable = false)
    private Integer amount;

    @NotNull(message = "Giá tiền không được để trống")
    @Min(value = 1, message = "Giá tiền phải lớn hơn 0")
    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "bonus_percentage")
    private Double bonusPercentage = 0.0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
