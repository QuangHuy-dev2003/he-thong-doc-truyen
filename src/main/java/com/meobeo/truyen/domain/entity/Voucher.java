package com.meobeo.truyen.domain.entity;

import com.meobeo.truyen.domain.enums.VoucherStatus;
import com.meobeo.truyen.domain.enums.VoucherType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vouchers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Mã voucher không được để trống")
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @NotBlank(message = "Tên voucher không được để trống")
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @NotNull(message = "Loại voucher không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private VoucherType type;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @NotNull(message = "Số tiền tối thiểu không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền tối thiểu phải lớn hơn 0")
    @Column(name = "min_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Trạng thái voucher không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VoucherStatus status = VoucherStatus.ACTIVE;

    @Min(value = 1, message = "Số lần sử dụng tối đa phải lớn hơn 0")
    @Column(name = "max_usage_count")
    private Integer maxUsageCount;

    @Min(value = 1, message = "Số người dùng tối đa phải lớn hơn 0")
    @Column(name = "max_users_count")
    private Integer maxUsersCount;

    @Min(value = 1, message = "Số lần sử dụng mỗi người dùng phải lớn hơn 0")
    @Column(name = "max_usage_per_user")
    private Integer maxUsagePerUser = 1;

    @NotNull(message = "Thời gian bắt đầu hiệu lực không được để trống")
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @NotNull(message = "Thời gian kết thúc hiệu lực không được để trống")
    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VoucherUsage> voucherUsages = new ArrayList<>();
}
