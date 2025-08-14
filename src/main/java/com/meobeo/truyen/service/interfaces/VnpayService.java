package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.entity.PaymentTransaction;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.topup.TopupPaymentRequest;
import com.meobeo.truyen.domain.response.topup.PaymentHistoryResponse;
import com.meobeo.truyen.domain.response.topup.PaymentResult;
import com.meobeo.truyen.domain.response.topup.TopupPaymentResponse;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface VnpayService {

    /**
     * Tạo URL thanh toán VNPAY
     */
    TopupPaymentResponse createPaymentUrl(TopupPaymentRequest request, User user);

    /**
     * Xử lý callback từ VNPAY
     */
    PaymentResult processPaymentCallback(Map<String, String> vnpayResponse);

    /**
     * Kiểm tra trạng thái giao dịch
     */
    PaymentResult checkPaymentStatus(String orderId, User user);

    /**
     * Lấy lịch sử giao dịch của user
     */
    PaymentHistoryResponse getPaymentHistory(User user, Pageable pageable);

    /**
     * Tạo mã đơn hàng
     */
    String generateOrderId(Long userId, Long packageId);

    /**
     * Tính toán giảm giá voucher
     */
    java.math.BigDecimal calculateVoucherDiscount(String voucherCode, java.math.BigDecimal originalAmount, Long userId);

    /**
     * Cập nhật ví người dùng
     */
    void updateUserWallet(User user, PaymentTransaction transaction);

    /**
     * Tạo lịch sử giao dịch ví
     */
    void createWalletTransaction(User user, PaymentTransaction transaction);

    /**
     * Xử lý giao dịch hết hạn
     */
    void processExpiredTransactions();
}
