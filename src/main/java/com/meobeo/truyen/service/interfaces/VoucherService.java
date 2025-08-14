package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.entity.Voucher;
import com.meobeo.truyen.domain.request.voucher.ApplyVoucherRequest;
import com.meobeo.truyen.domain.request.voucher.CreateVoucherRequest;
import com.meobeo.truyen.domain.request.voucher.UpdateVoucherRequest;
import com.meobeo.truyen.domain.response.voucher.*;
import org.springframework.data.domain.Pageable;

public interface VoucherService {

    /**
     * Tạo voucher mới (ADMIN)
     */
    VoucherResponse createVoucher(CreateVoucherRequest request);

    /**
     * Cập nhật voucher (ADMIN)
     */
    VoucherResponse updateVoucher(Long id, UpdateVoucherRequest request);

    /**
     * Xóa voucher (ADMIN)
     */
    void deleteVoucher(Long id);

    /**
     * Lấy tất cả voucher với phân trang (ADMIN)
     */
    VoucherListResponse getAllVouchers(Pageable pageable);

    /**
     * Lấy voucher theo ID (ADMIN)
     */
    VoucherResponse getVoucherById(Long id);

    /**
     * Áp dụng voucher cho topup (USER)
     */
    VoucherUsageResponse applyVoucher(ApplyVoucherRequest request, Long userId);

    /**
     * Lấy lịch sử sử dụng voucher (ADMIN)
     */
    VoucherUsageListResponse getVoucherUsageHistory(Long voucherId, Pageable pageable);

    /**
     * Kiểm tra voucher có hợp lệ không
     */
    boolean isValidVoucher(String voucherCode, Long userId);

    /**
     * Tính toán giảm giá (USER)
     */
    DiscountCalculationResponse calculateDiscount(String voucherCode, java.math.BigDecimal amount);

    /**
     * Lấy voucher theo mã
     */
    Voucher getVoucherByCode(String code);
}
