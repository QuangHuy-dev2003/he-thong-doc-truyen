package com.meobeo.truyen.controller.giftcode;

import com.meobeo.truyen.domain.request.giftcode.CreateGiftCodeRequest;
import com.meobeo.truyen.domain.request.giftcode.UpdateGiftCodeRequest;
import com.meobeo.truyen.domain.request.giftcode.UseGiftCodeRequest;
import com.meobeo.truyen.domain.response.giftcode.*;
import com.meobeo.truyen.service.interfaces.GiftCodeService;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class GiftCodeController {

    private final GiftCodeService giftCodeService;
    private final SecurityUtils securityUtils;

    /**
     * Tạo gift code mới (ADMIN)
     */
    @PostMapping("/admin/gift-codes/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GiftCodeResponse>> createGiftCode(
            @Valid @RequestBody CreateGiftCodeRequest request) {
        log.info("Admin tạo gift code mới: {}", request.getCode());

        GiftCodeResponse response = giftCodeService.createGiftCode(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo gift code thành công", response));
    }

    /**
     * Cập nhật gift code (ADMIN)
     */
    @PutMapping("/admin/gift-codes/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GiftCodeResponse>> updateGiftCode(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGiftCodeRequest request) {
        log.info("Admin cập nhật gift code: {}", id);

        GiftCodeResponse response = giftCodeService.updateGiftCode(id, request);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật gift code thành công", response));
    }

    /**
     * Xóa gift code (ADMIN)
     */
    @DeleteMapping("/admin/gift-codes/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGiftCode(@PathVariable Long id) {
        log.info("Admin xóa gift code: {}", id);

        giftCodeService.deleteGiftCode(id);

        return ResponseEntity.ok(ApiResponse.success("Xóa gift code thành công", null));
    }

    /**
     * Lấy tất cả gift codes đang active (ADMIN)
     */
    @GetMapping("/admin/gift-codes/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GiftCodeListResponse>> getAllActiveGiftCodes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Admin lấy danh sách gift codes, page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        GiftCodeListResponse response = giftCodeService.getAllActiveGiftCodes(pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gift codes thành công", response));
    }

    /**
     * Lấy gift code theo ID (ADMIN)
     */
    @GetMapping("/admin/gift-codes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GiftCodeResponse>> getGiftCodeById(@PathVariable Long id) {
        log.info("Admin lấy gift code theo ID: {}", id);

        GiftCodeResponse response = giftCodeService.getGiftCodeById(id);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin gift code thành công", response));
    }

    /**
     * Sử dụng gift code (USER)
     */
    @PostMapping("/gift-codes/use")
    public ResponseEntity<ApiResponse<GiftCodeUsageResponse>> useGiftCode(
            @Valid @RequestBody UseGiftCodeRequest request) {
        Long userId = securityUtils.getCurrentUserIdOrThrow();
        log.info("User {} sử dụng gift code: {}", userId, request.getCode());

        GiftCodeUsageResponse response = giftCodeService.useGiftCode(request, userId);

        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    /**
     * Lấy lịch sử sử dụng gift code (ADMIN)
     */
    @GetMapping("/admin/gift-codes/{id}/usage-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GiftCodeUsageListResponse>> getGiftCodeUsageHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Admin lấy lịch sử sử dụng gift code: {}", id);

        Pageable pageable = PageRequest.of(page, size);
        GiftCodeUsageListResponse response = giftCodeService.getGiftCodeUsageHistory(id, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử sử dụng thành công", response));
    }
}
