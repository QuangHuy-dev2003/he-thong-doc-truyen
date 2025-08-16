package com.meobeo.truyen.controller.user;

import com.meobeo.truyen.domain.response.wallet.UserWalletInfoResponse;
import com.meobeo.truyen.domain.response.wallet.UserWalletTransactionListResponse;
import com.meobeo.truyen.service.interfaces.AdminWalletService;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class UserWalletController {

    private final AdminWalletService adminWalletService;
    private final SecurityUtils securityUtils;

    /**
     * Lấy thông tin ví của người dùng hiện tại
     */
    @GetMapping("/wallets/my-wallet")
    public ResponseEntity<ApiResponse<UserWalletInfoResponse>> getMyWalletInfo() {
        Long userId = securityUtils.getCurrentUserIdOrThrow();
        log.info("User {} lấy thông tin ví của bản thân", userId);

        UserWalletInfoResponse response = adminWalletService.getUserWalletInfo(userId);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin ví thành công", response));
    }

    /**
     * Lấy lịch sử giao dịch ví của người dùng hiện tại
     */
    @GetMapping("/wallets/my-transactions")
    public ResponseEntity<ApiResponse<UserWalletTransactionListResponse>> getMyWalletTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = securityUtils.getCurrentUserIdOrThrow();
        log.info("User {} lấy lịch sử giao dịch ví, page: {}, size: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        UserWalletTransactionListResponse response = adminWalletService.getUserWalletTransactions(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử giao dịch thành công", response));
    }
}
