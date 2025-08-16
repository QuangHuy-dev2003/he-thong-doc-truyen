package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.wallet.AdminWalletAdjustmentRequest;
import com.meobeo.truyen.domain.response.wallet.AdminWalletAdjustmentResponse;
import com.meobeo.truyen.domain.response.wallet.UserWalletInfoResponse;
import com.meobeo.truyen.domain.response.wallet.UserWalletListResponse;
import com.meobeo.truyen.domain.response.wallet.UserWalletTransactionListResponse;
import org.springframework.data.domain.Pageable;

public interface AdminWalletService {

    /**
     * Điều chỉnh ví người dùng (cộng/trừ tiền)
     */
    AdminWalletAdjustmentResponse adjustUserWallet(AdminWalletAdjustmentRequest request, String adminUsername);

    /**
     * Lấy thông tin ví người dùng
     */
    UserWalletInfoResponse getUserWalletInfo(Long userId);

    /**
     * Lấy lịch sử giao dịch ví của người dùng
     */
    UserWalletTransactionListResponse getUserWalletTransactions(Long userId, Pageable pageable);

    /**
     * Lấy lịch sử giao dịch ví của tất cả người dùng (cho ADMIN)
     */
    UserWalletTransactionListResponse getAllWalletTransactions(Pageable pageable);

    /**
     * Lấy danh sách tất cả ví người dùng (cho ADMIN)
     */
    UserWalletListResponse getAllUserWallets(Pageable pageable);
}
