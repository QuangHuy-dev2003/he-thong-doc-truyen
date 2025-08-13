package com.meobeo.truyen.domain.entity;

import com.meobeo.truyen.domain.enums.GiftCodeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gift_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiftCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Mã quà tặng không được để trống")
    @Column(unique = true, nullable = false)
    private String code;

    @NotBlank(message = "Tên quà tặng không được để trống")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Số lượng linh thạch không được để trống")
    @Min(value = 1, message = "Số lượng linh thạch phải lớn hơn 0")
    @Column(nullable = false)
    private Integer amount;

    @NotNull(message = "Loại quà tặng không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GiftCodeType type;

    @Min(value = 0, message = "Số lượt sử dụng tối đa không được âm")
    @Column(name = "max_usage_count")
    private Integer maxUsageCount;

    @Min(value = 0, message = "Số người sử dụng tối đa không được âm")
    @Column(name = "max_users_count")
    private Integer maxUsersCount;

    @Min(value = 1, message = "Số lượt sử dụng tối đa mỗi người phải lớn hơn 0")
    @Column(name = "max_usage_per_user", nullable = false)
    private Integer maxUsagePerUser = 1;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "giftCode", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GiftCodeUsage> usages = new ArrayList<>();
}
