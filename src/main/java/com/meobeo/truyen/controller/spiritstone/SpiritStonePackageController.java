package com.meobeo.truyen.controller.spiritstone;

import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.entity.WalletTransaction;
import com.meobeo.truyen.domain.request.spiritstone.CreateSpiritStonePackageRequest;
import com.meobeo.truyen.domain.request.spiritstone.ExchangeSpiritStoneByAmountRequest;
import com.meobeo.truyen.domain.request.spiritstone.ExchangeSpiritStoneByPackageRequest;
import com.meobeo.truyen.domain.request.spiritstone.UpdateSpiritStonePackageRequest;
import com.meobeo.truyen.domain.response.spiritstone.ExchangeHistoryListResponse;
import com.meobeo.truyen.domain.response.spiritstone.SpiritStonePackageListResponse;
import com.meobeo.truyen.domain.response.spiritstone.SpiritStonePackageResponse;
import com.meobeo.truyen.domain.response.spiritstone.WalletBalanceResponse;
import com.meobeo.truyen.service.interfaces.SpiritStonePackageService;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class SpiritStonePackageController {

    private final SpiritStonePackageService spiritStonePackageService;
    private final SecurityUtils securityUtils;

    // ADMIN endpoints

    /**
     * Tạo gói đổi linh thạch mới (ADMIN)
     */
    @PostMapping("/spirit-stone-packages/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SpiritStonePackageResponse>> createSpiritStonePackage(
            @Valid @RequestBody CreateSpiritStonePackageRequest request) {
        log.info("ADMIN tạo gói đổi linh thạch mới: {}", request.getName());

        SpiritStonePackageResponse response = spiritStonePackageService.createSpiritStonePackage(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo gói đổi linh thạch thành công", response));
    }

    /**
     * Cập nhật gói đổi linh thạch (ADMIN)
     */
    @PutMapping("/spirit-stone-packages/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SpiritStonePackageResponse>> updateSpiritStonePackage(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSpiritStonePackageRequest request) {
        log.info("ADMIN cập nhật gói đổi linh thạch với ID: {}", id);

        SpiritStonePackageResponse response = spiritStonePackageService.updateSpiritStonePackage(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật gói đổi linh thạch thành công", response));
    }

    /**
     * Xóa gói đổi linh thạch (ADMIN)
     */
    @DeleteMapping("/spirit-stone-packages/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSpiritStonePackage(@PathVariable Long id) {
        log.info("ADMIN xóa gói đổi linh thạch với ID: {}", id);

        spiritStonePackageService.deleteSpiritStonePackage(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa gói đổi linh thạch thành công", null));
    }

    // USER endpoints

    /**
     * Xem tất cả gói đổi linh thạch đang hoạt động (USER)
     */
    @GetMapping("/spirit-stone-packages/all")
    public ResponseEntity<ApiResponse<SpiritStonePackageListResponse>> getAllActivePackages() {
        log.info("USER xem tất cả gói đổi linh thạch đang hoạt động");

        SpiritStonePackageListResponse response = spiritStonePackageService.getAllActivePackages();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gói đổi linh thạch thành công", response));
    }

    /**
     * Xem chi tiết gói đổi linh thạch (USER)
     */
    @GetMapping("/spirit-stone-packages/get/{id}")
    public ResponseEntity<ApiResponse<SpiritStonePackageResponse>> getPackageById(@PathVariable Long id) {
        log.info("USER xem chi tiết gói đổi linh thạch với ID: {}", id);

        SpiritStonePackageResponse response = spiritStonePackageService.getPackageById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết gói đổi linh thạch thành công", response));
    }

    /**
     * Thực hiện đổi linh thạch theo gói (USER)
     */
    @PostMapping("/spirit-stone-packages/exchange-by-package")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> exchangeSpiritStoneByPackage(
            @Valid @RequestBody ExchangeSpiritStoneByPackageRequest request) {
        Long userId = securityUtils.getCurrentUserIdOrThrow();
        log.info("USER thực hiện đổi linh thạch theo gói. User ID: {}, Package ID: {}", userId, request.getPackageId());

        WalletBalanceResponse response = spiritStonePackageService.exchangeSpiritStoneByPackage(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Đổi linh thạch theo gói thành công", response));
    }

    /**
     * Thực hiện đổi linh thạch theo số tiền (USER)
     */
    @PostMapping("/spirit-stone-packages/exchange-by-amount")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> exchangeSpiritStoneByAmount(
            @Valid @RequestBody ExchangeSpiritStoneByAmountRequest request) {
        Long userId = securityUtils.getCurrentUserIdOrThrow();
        log.info("USER thực hiện đổi linh thạch theo số tiền. User ID: {}, Amount: {}", userId, request.getAmount());

        WalletBalanceResponse response = spiritStonePackageService.exchangeSpiritStoneByAmount(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Đổi linh thạch theo số tiền thành công", response));
    }

    /**
     * Xem lịch sử đổi linh thạch (USER) - Từ WalletTransaction
     */
    @GetMapping("/spirit-stone-packages/history")
    public ResponseEntity<ApiResponse<ExchangeHistoryListResponse>> getExchangeHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = securityUtils.getCurrentUserIdOrThrow();
        log.info("USER xem lịch sử đổi linh thạch. User ID: {}, Page: {}, Size: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        ExchangeHistoryListResponse response = spiritStonePackageService.getExchangeHistory(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đổi linh thạch thành công", response));
    }
}
