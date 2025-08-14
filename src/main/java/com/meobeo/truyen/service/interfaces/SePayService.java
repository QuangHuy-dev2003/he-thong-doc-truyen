package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.topup.CreateSePayTopupRequest;
import com.meobeo.truyen.domain.request.topup.SePayWebhookRequest;
import com.meobeo.truyen.domain.response.topup.SePayTopupHistoryResponse;
import com.meobeo.truyen.domain.response.topup.SePayTopupResponse;
import com.meobeo.truyen.domain.entity.SePayTopupRequest;
import org.springframework.data.domain.Pageable;

public interface SePayService {

    /**
     * Tạo yêu cầu nạp tiền và trả về QR code
     */
    SePayTopupResponse createTopupRequest(CreateSePayTopupRequest request, User user);

    /**
     * Xử lý webhook từ SePay
     */
    boolean processWebhook(SePayWebhookRequest webhookRequest);

    /**
     * Lấy lịch sử nạp tiền của user
     */
    SePayTopupHistoryResponse getTopupHistory(User user, Pageable pageable);

    /**
     * Xử lý các yêu cầu nạp tiền hết hạn
     */
    void processExpiredRequests();

    /**
     * Kiểm tra trạng thái yêu cầu nạp tiền
     */
    SePayTopupRequest checkTopupStatus(Long requestId, Long userId);
}
