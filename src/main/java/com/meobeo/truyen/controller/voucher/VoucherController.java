package com.meobeo.truyen.controller.voucher;

import com.meobeo.truyen.domain.request.voucher.ApplyVoucherRequest;
import com.meobeo.truyen.domain.request.voucher.CreateVoucherRequest;
import com.meobeo.truyen.domain.request.voucher.UpdateVoucherRequest;
import com.meobeo.truyen.domain.response.voucher.*;
import com.meobeo.truyen.service.interfaces.VoucherService;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class VoucherController {

    private final VoucherService voucherService;
    private final SecurityUtils securityUtils;

    /**
     * Tạo voucher mới (ADMIN)
     */
    @PostMapping("/admin/vouchers/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(
            @Valid @RequestBody CreateVoucherRequest request) {
        log.info("Admin tạo voucher mới với mã: {}", request.getCode());

        VoucherResponse response = voucherService.createVoucher(request);

        return ResponseEntity.ok(ApiResponse.success("Tạo voucher thành công", response));
    }

    /**
     * Cập nhật voucher (ADMIN)
     */
    @PutMapping("/admin/vouchers/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVoucherRequest request) {
        log.info("Admin cập nhật voucher với ID: {}", id);

        VoucherResponse response = voucherService.updateVoucher(id, request);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật voucher thành công", response));
    }

    /**
     * Xóa voucher (ADMIN)
     */
    @DeleteMapping("/admin/vouchers/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(@PathVariable Long id) {
        log.info("Admin xóa voucher với ID: {}", id);

        voucherService.deleteVoucher(id);

        return ResponseEntity.ok(ApiResponse.success("Xóa voucher thành công", null));
    }

    /**
     * Lấy tất cả voucher với phân trang (ADMIN)
     */
    @GetMapping("/admin/vouchers/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoucherListResponse>> getAllVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Admin lấy danh sách voucher - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        VoucherListResponse response = voucherService.getAllVouchers(pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách voucher thành công", response));
    }

    /**
     * Lấy voucher theo ID (ADMIN)
     */
    @GetMapping("/admin/vouchers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucherById(@PathVariable Long id) {
        log.info("Admin lấy voucher theo ID: {}", id);

        VoucherResponse response = voucherService.getVoucherById(id);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin voucher thành công", response));
    }

    /**
     * Áp dụng voucher cho topup (USER)
     */
    @PostMapping("/vouchers/apply")

    public ResponseEntity<ApiResponse<VoucherUsageResponse>> applyVoucher(
            @Valid @RequestBody ApplyVoucherRequest request) {
        Long userId = securityUtils.getCurrentUserIdOrThrow();
        log.info("User {} áp dụng voucher: {}", userId, request.getVoucherCode());

        VoucherUsageResponse response = voucherService.applyVoucher(request, userId);

        return ResponseEntity.ok(ApiResponse.success("Áp dụng voucher thành công", response));
    }

    /**
     * Lấy lịch sử sử dụng voucher (ADMIN)
     */
    @GetMapping("/admin/vouchers/{id}/usage-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoucherUsageListResponse>> getVoucherUsageHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Admin lấy lịch sử sử dụng voucher ID: {} - page: {}, size: {}", id, page, size);

        Pageable pageable = PageRequest.of(page, size);
        VoucherUsageListResponse response = voucherService.getVoucherUsageHistory(id, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử sử dụng voucher thành công", response));
    }

    /**
     * Tính toán giảm giá (USER)
     */
    @PostMapping("/vouchers/calculate-discount")
    public ResponseEntity<ApiResponse<DiscountCalculationResponse>> calculateDiscount(
            @RequestParam String voucherCode,
            @RequestParam BigDecimal amount) {
        log.info("User tính toán giảm giá cho voucher: {} với số tiền: {}", voucherCode, amount);

        DiscountCalculationResponse response = voucherService.calculateDiscount(voucherCode, amount);

        return ResponseEntity.ok(ApiResponse.success("Tính toán giảm giá thành công", response));
    }
}
