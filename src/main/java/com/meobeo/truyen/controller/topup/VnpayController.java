package com.meobeo.truyen.controller.topup;

import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.topup.TopupPaymentRequest;
import com.meobeo.truyen.domain.response.topup.PaymentHistoryResponse;
import com.meobeo.truyen.domain.response.topup.PaymentResult;
import com.meobeo.truyen.domain.response.topup.TopupPaymentResponse;
import com.meobeo.truyen.security.CustomUserDetails;
import com.meobeo.truyen.service.interfaces.VnpayService;
import com.meobeo.truyen.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
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
public class VnpayController {

    private final VnpayService vnpayService;

    /**
     * Tạo URL thanh toán VNPAY
     */
    @PostMapping("/topup/vnpay/create")
    public ResponseEntity<ApiResponse<TopupPaymentResponse>> createPaymentUrl(
            @Valid @RequestBody TopupPaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Kiểm tra null safety
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Bạn cần đăng nhập để thực hiện thao tác này"));
        }

        User user = userDetails.getUser();
        log.info("User {} tạo URL thanh toán VNPAY cho gói {}", user.getId(), request.getPackageId());

        try {
            TopupPaymentResponse response = vnpayService.createPaymentUrl(request, user);
            return ResponseEntity.ok(ApiResponse.success("Tạo URL thanh toán thành công", response));
        } catch (Exception e) {
            log.error("Lỗi tạo URL thanh toán VNPAY", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi tạo URL thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Xử lý callback từ VNPAY
     */
    @GetMapping("/vnpay/payment-callback")
    public ResponseEntity<String> processPaymentCallback(HttpServletRequest request) {
        log.info("Nhận callback từ VNPAY");

        try {
            // Lấy tất cả parameter từ request
            Map<String, String> vnpayResponse = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values.length > 0) {
                    vnpayResponse.put(key, values[0]);
                }
            });

            log.info("Callback parameters: {}", vnpayResponse);

            PaymentResult result = vnpayService.processPaymentCallback(vnpayResponse);

            if (result.isSuccess()) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", "/payment/success?orderId=" + result.getOrderId())
                        .build();
            } else {
                String safeMessage = java.net.URLEncoder.encode(
                        result.getMessage() != null ? result.getMessage() : "Payment failed",
                        java.nio.charset.StandardCharsets.UTF_8);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location",
                                "/payment/failed?orderId=" + result.getOrderId() + "&message=" + safeMessage)
                        .build();
            }

        } catch (Exception e) {
            log.error("Lỗi xử lý callback VNPAY", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/payment/error?message=Lỗi xử lý thanh toán")
                    .build();
        }
    }

    /**
     * Kiểm tra trạng thái giao dịch
     */
    @GetMapping("/topup/payment/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResult>> checkPaymentStatus(
            @PathVariable String orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Bạn cần đăng nhập để thực hiện thao tác này"));
        }

        User user = userDetails.getUser();
        log.info("User {} kiểm tra trạng thái giao dịch orderId: {}", user.getId(), orderId);

        try {
            PaymentResult result = vnpayService.checkPaymentStatus(orderId, user);
            return ResponseEntity.ok(ApiResponse.success("Kiểm tra trạng thái giao dịch thành công", result));
        } catch (Exception e) {
            log.error("Lỗi kiểm tra trạng thái giao dịch", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi kiểm tra trạng thái giao dịch: " + e.getMessage()));
        }
    }

    /**
     * Lấy lịch sử giao dịch của user
     */
    @GetMapping("/topup/payment/history")
    public ResponseEntity<ApiResponse<PaymentHistoryResponse>> getPaymentHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Bạn cần đăng nhập để thực hiện thao tác này"));
        }

        User user = userDetails.getUser();
        log.info("User {} lấy lịch sử giao dịch - page: {}, size: {}", user.getId(), page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            PaymentHistoryResponse response = vnpayService.getPaymentHistory(user, pageable);
            return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử giao dịch thành công", response));
        } catch (Exception e) {
            log.error("Lỗi lấy lịch sử giao dịch", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy lịch sử giao dịch: " + e.getMessage()));
        }
    }

    /**
     * Webhook endpoint cho VNPAY (nếu cần)
     */
    @PostMapping("/vnpay/webhook")
    public ResponseEntity<String> webhookCallback(HttpServletRequest request) {
        log.info("Nhận webhook từ VNPAY");

        try {
            // Lấy tất cả parameter từ request
            Map<String, String> vnpayResponse = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values.length > 0) {
                    vnpayResponse.put(key, values[0]);
                }
            });

            log.info("Webhook parameters: {}", vnpayResponse);

            PaymentResult result = vnpayService.processPaymentCallback(vnpayResponse);

            if (result.isSuccess()) {
                return ResponseEntity.ok("OK");
            } else {
                return ResponseEntity.badRequest().body("FAILED");
            }

        } catch (Exception e) {
            log.error("Lỗi xử lý webhook VNPAY", e);
            return ResponseEntity.badRequest().body("ERROR");
        }
    }
}
