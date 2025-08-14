package com.meobeo.truyen.domain.request.voucher;

import com.meobeo.truyen.domain.enums.VoucherType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateVoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 50, message = "Mã voucher không được quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên voucher không được để trống")
    @Size(max = 200, message = "Tên voucher không được quá 200 ký tự")
    private String name;

    @NotNull(message = "Loại voucher không được để trống")
    private VoucherType type;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;

    @NotNull(message = "Số tiền tối thiểu không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền tối thiểu phải lớn hơn 0")
    private BigDecimal minAmount;

    @DecimalMin(value = "0.01", message = "Số tiền giảm tối đa phải lớn hơn 0")
    private BigDecimal maxDiscountAmount;

    @Min(value = 1, message = "Số lần sử dụng tối đa phải lớn hơn 0")
    private Integer maxUsageCount;

    @Min(value = 1, message = "Số người dùng tối đa phải lớn hơn 0")
    private Integer maxUsersCount;

    @Min(value = 1, message = "Số lần sử dụng mỗi người dùng phải lớn hơn 0")
    private Integer maxUsagePerUser = 1;

    @NotNull(message = "Thời gian bắt đầu hiệu lực không được để trống")
    @Future(message = "Thời gian bắt đầu hiệu lực phải trong tương lai")
    private LocalDateTime validFrom;

    @NotNull(message = "Thời gian kết thúc hiệu lực không được để trống")
    @Future(message = "Thời gian kết thúc hiệu lực phải trong tương lai")
    private LocalDateTime validUntil;

    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    private String description;
}
