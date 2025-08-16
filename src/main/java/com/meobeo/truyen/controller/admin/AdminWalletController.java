package com.meobeo.truyen.controller.admin;

import com.meobeo.truyen.domain.request.wallet.AdminWalletAdjustmentRequest;
import com.meobeo.truyen.domain.response.wallet.AdminWalletAdjustmentResponse;
import com.meobeo.truyen.domain.response.wallet.UserWalletInfoResponse;
import com.meobeo.truyen.domain.response.wallet.UserWalletListResponse;
import com.meobeo.truyen.domain.response.wallet.UserWalletTransactionListResponse;
import com.meobeo.truyen.service.interfaces.AdminWalletService;
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

@RestController
@RequestMapping("/api/v1/admin/wallets")
@RequiredArgsConstructor
@Slf4j
public class AdminWalletController {

    private final AdminWalletService adminWalletService;
    private final SecurityUtils securityUtils;

    /**
     * Điều chỉnh ví người dùng (cộng/trừ tiền)
     */
    @PostMapping("/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminWalletAdjustmentResponse>> adjustUserWallet(
            @Valid @RequestBody AdminWalletAdjustmentRequest request) {
        String adminUsername = securityUtils.getCurrentUsernameOrThrow();
        log.info("Admin {} điều chỉnh ví cho user {}: {} {} {}",
                adminUsername, request.getUserId(), request.getAdjustmentType(),
                request.getAmount(), request.getCurrency());

        AdminWalletAdjustmentResponse response = adminWalletService.adjustUserWallet(request, adminUsername);

        return ResponseEntity.ok(ApiResponse.success("Điều chỉnh ví thành công", response));
    }

    /**
     * Lấy thông tin ví người dùng
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserWalletInfoResponse>> getUserWalletInfo(@PathVariable Long userId) {
        log.info("Admin lấy thông tin ví cho user ID: {}", userId);

        UserWalletInfoResponse response = adminWalletService.getUserWalletInfo(userId);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin ví thành công", response));
    }

    /**
     * Lấy lịch sử giao dịch ví của người dùng
     */
    @GetMapping("/users/{userId}/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserWalletTransactionListResponse>> getUserWalletTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy lịch sử giao dịch ví cho user ID: {}, page: {}, size: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        UserWalletTransactionListResponse response = adminWalletService.getUserWalletTransactions(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử giao dịch thành công", response));
    }

    /**
     * Lấy tất cả lịch sử giao dịch ví (cho ADMIN)
     */
    @GetMapping("/transactions/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserWalletTransactionListResponse>> getAllWalletTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy tất cả lịch sử giao dịch ví, page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        UserWalletTransactionListResponse response = adminWalletService.getAllWalletTransactions(pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy tất cả lịch sử giao dịch thành công", response));
    }

    /**
     * Lấy danh sách tất cả ví người dùng
     */
    @GetMapping("/users/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserWalletListResponse>> getAllUserWallets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy danh sách tất cả ví người dùng, page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        UserWalletListResponse response = adminWalletService.getAllUserWallets(pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ví người dùng thành công", response));
    }
}
