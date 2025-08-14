package com.meobeo.truyen.domain.response.topup;

import com.meobeo.truyen.domain.entity.PaymentTransaction;
import lombok.Data;

@Data
public class PaymentResult {

    private boolean success;
    private String message;
    private PaymentTransaction transaction;
    private String orderId;
    private String status;
}
