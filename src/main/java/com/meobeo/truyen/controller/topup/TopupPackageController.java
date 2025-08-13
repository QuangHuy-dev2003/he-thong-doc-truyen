package com.meobeo.truyen.controller.topup;

import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.topup.CreateTopupPackageRequest;
import com.meobeo.truyen.domain.request.topup.TopupRequest;
import com.meobeo.truyen.domain.request.topup.UpdateTopupPackageRequest;
import com.meobeo.truyen.domain.response.topup.TopupPackageListResponse;
import com.meobeo.truyen.domain.response.topup.TopupPackageResponse;
import com.meobeo.truyen.service.TopupPackageService;
import com.meobeo.truyen.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TopupPackageController {

    private final TopupPackageService topupPackageService;

    /**
     * Tạo gói nạp tiền mới (ADMIN)
     */
    @PostMapping("topup-packages/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TopupPackageResponse>> createTopupPackage(
            @Valid @RequestBody CreateTopupPackageRequest request) {
        log.info("Admin tạo gói nạp tiền mới: {}", request.getName());

        TopupPackageResponse response = topupPackageService.createTopupPackage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo gói nạp tiền thành công", response));
    }

    /**
     * Cập nhật gói nạp tiền (ADMIN)
     */
    @PutMapping("topup-packages/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TopupPackageResponse>> updateTopupPackage(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTopupPackageRequest request) {
        log.info("Admin cập nhật gói nạp tiền với ID: {}", id);

        TopupPackageResponse response = topupPackageService.updateTopupPackage(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật gói nạp tiền thành công", response));
    }

    /**
     * Xóa gói nạp tiền (ADMIN)
     */
    @DeleteMapping("topup-packages/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTopupPackage(@PathVariable Long id) {
        log.info("Admin xóa gói nạp tiền với ID: {}", id);

        topupPackageService.deleteTopupPackage(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa gói nạp tiền thành công", null));
    }

    /**
     * Xem danh sách gói nạp tiền (Public)
     */
    @GetMapping("topup-packages/all")
    public ResponseEntity<ApiResponse<TopupPackageListResponse>> getAllActivePackages() {
        log.info("Lấy danh sách gói nạp tiền đang hoạt động");

        TopupPackageListResponse response = topupPackageService.getAllActivePackages();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gói nạp tiền thành công", response));
    }

    /**
     * Xem chi tiết gói nạp tiền (Public)
     */
    @GetMapping("topup-packages/get/{id}")
    public ResponseEntity<ApiResponse<TopupPackageResponse>> getPackageById(@PathVariable Long id) {
        log.info("Lấy chi tiết gói nạp tiền với ID: {}", id);

        TopupPackageResponse response = topupPackageService.getPackageById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết gói nạp tiền thành công", response));
    }

    /**
     * Nạp tiền (USER)
     */
    @PostMapping("/topup/process")
    public ResponseEntity<ApiResponse<Void>> processTopup(
            @Valid @RequestBody TopupRequest request,
            @AuthenticationPrincipal User user) {
        log.info("User {} nạp tiền với gói ID: {}", user.getId(), request.getPackageId());

        topupPackageService.processTopup(request, user);
        return ResponseEntity.ok(ApiResponse.success("Nạp tiền thành công", null));
    }
}
