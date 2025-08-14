package com.meobeo.truyen.controller.topup;

import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.topup.CreateSePayTopupRequest;
import com.meobeo.truyen.domain.request.topup.SePayWebhookRequest;
import com.meobeo.truyen.domain.response.topup.SePayTopupHistoryResponse;
import com.meobeo.truyen.domain.response.topup.SePayTopupResponse;
import com.meobeo.truyen.security.CustomUserDetails;
import com.meobeo.truyen.service.interfaces.SePayService;
import com.meobeo.truyen.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class SePayController {

    private final SePayService sePayService;

    /**
     * Tạo QR code nạp tiền SePay
     */
    @PostMapping("/topup/sepay/create")
    public ResponseEntity<ApiResponse<SePayTopupResponse>> createTopupRequest(
            @Valid @RequestBody CreateSePayTopupRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Kiểm tra null safety
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Bạn cần đăng nhập để thực hiện thao tác này"));
        }

        User user = userDetails.getUser();
        log.info("User {} tạo yêu cầu nạp tiền SePay với gói {}", user.getId(), request.getPackageId());

        try {
            SePayTopupResponse response = sePayService.createTopupRequest(request, user);
            return ResponseEntity.ok(ApiResponse.success("Tạo QR code nạp tiền thành công", response));
        } catch (Exception e) {
            log.error("Lỗi tạo QR code nạp tiền SePay", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi tạo QR code nạp tiền: " + e.getMessage()));
        }
    }

    /**
     * Webhook endpoint cho SePay
     */
    @PostMapping("/sepay/webhook")
    public ResponseEntity<Map<String, Object>> webhookCallback(@RequestBody SePayWebhookRequest webhookRequest) {
        log.info("=== NHẬN WEBHOOK TỪ SEPAY ===");
        log.info("Gateway: {}", webhookRequest.getGateway());
        log.info("Transaction ID: {}", webhookRequest.getId());
        log.info("Amount: {}", webhookRequest.getTransferAmount());
        log.info("Content: {}", webhookRequest.getContent());
        log.info("Transfer Type: {}", webhookRequest.getTransferType());
        log.info("Reference Code: {}", webhookRequest.getReferenceCode());
        log.info("================================");

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = sePayService.processWebhook(webhookRequest);

            if (success) {
                response.put("success", true);
                response.put("message", "Xử lý webhook thành công");
                log.info("✅ Xử lý webhook thành công cho transaction ID: {}", webhookRequest.getId());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Xử lý webhook thất bại");
                log.warn("❌ Xử lý webhook thất bại cho transaction ID: {}", webhookRequest.getId());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("💥 Lỗi xử lý webhook SePay cho transaction ID: {}", webhookRequest.getId(), e);
            response.put("success", false);
            response.put("message", "Lỗi xử lý webhook: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy lịch sử nạp tiền SePay của user
     */
    @GetMapping("/topup/sepay/history")
    public ResponseEntity<ApiResponse<SePayTopupHistoryResponse>> getTopupHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Bạn cần đăng nhập để thực hiện thao tác này"));
        }

        User user = userDetails.getUser();
        log.info("User {} lấy lịch sử nạp tiền SePay - page: {}, size: {}", user.getId(), page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            SePayTopupHistoryResponse response = sePayService.getTopupHistory(user, pageable);
            return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử nạp tiền thành công", response));
        } catch (Exception e) {
            log.error("Lỗi lấy lịch sử nạp tiền SePay", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy lịch sử nạp tiền: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra trạng thái yêu cầu nạp tiền
     */
    @GetMapping("/topup/sepay/status/{requestId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkTopupStatus(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Bạn cần đăng nhập để thực hiện thao tác này"));
        }

        User user = userDetails.getUser();
        log.info("User {} kiểm tra trạng thái yêu cầu nạp tiền ID: {}", user.getId(), requestId);

        try {
            var topupRequest = sePayService.checkTopupStatus(requestId, user.getId());

            Map<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("requestId", topupRequest.getId());
            statusInfo.put("amount", topupRequest.getAmount());
            statusInfo.put("transferContent", topupRequest.getTransferContent());
            statusInfo.put("status", topupRequest.getStatus().name());
            statusInfo.put("createdAt", topupRequest.getCreatedAt());
            statusInfo.put("processedAt", topupRequest.getProcessedAt());

            return ResponseEntity.ok(ApiResponse.success("Kiểm tra trạng thái thành công", statusInfo));
        } catch (Exception e) {
            log.error("Lỗi kiểm tra trạng thái yêu cầu nạp tiền", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi kiểm tra trạng thái: " + e.getMessage()));
        }
    }
}
