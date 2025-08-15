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
@Table(name = "spirit_stone_packages")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class SpiritStonePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên gói không được để trống")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Số linh thạch không được để trống")
    @Min(value = 1, message = "Số linh thạch phải lớn hơn 0")
    @Column(name = "spirit_stones", nullable = false)
    private Integer spiritStones;

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
