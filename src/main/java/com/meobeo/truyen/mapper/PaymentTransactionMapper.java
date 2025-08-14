package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.PaymentTransaction;
import com.meobeo.truyen.domain.response.topup.PaymentTransactionResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentTransactionMapper {

    /**
     * Chuyển đổi từ PaymentTransaction entity sang PaymentTransactionResponse
     */
    public PaymentTransactionResponse toResponse(PaymentTransaction entity) {
        PaymentTransactionResponse response = new PaymentTransactionResponse();
        response.setId(entity.getId());
        response.setOrderId(entity.getOrderId());
        response.setVnpayTransactionId(entity.getVnpayTransactionId());
        response.setAmount(entity.getAmount());
        response.setOriginalAmount(entity.getOriginalAmount());
        response.setDiscountAmount(entity.getDiscountAmount());
        response.setVoucherCode(entity.getVoucherCode());
        response.setStatus(entity.getStatus().name());
        response.setPaymentUrl(entity.getPaymentUrl());
        response.setVnpayResponseCode(entity.getVnpayResponseCode());
        response.setVnpayResponseMessage(entity.getVnpayResponseMessage());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setPaidAt(entity.getPaidAt());

        // Thông tin gói nạp tiền
        if (entity.getTopupPackage() != null) {
            response.setPackageId(entity.getTopupPackage().getId());
            response.setPackageName(entity.getTopupPackage().getName());
        }

        // Thông tin user
        if (entity.getUser() != null) {
            response.setUserId(entity.getUser().getId());
            response.setUsername(entity.getUser().getUsername());
        }

        return response;
    }

    /**
     * Chuyển đổi danh sách PaymentTransaction sang danh sách
     * PaymentTransactionResponse
     */
    public List<PaymentTransactionResponse> toResponseList(List<PaymentTransaction> entities) {
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
