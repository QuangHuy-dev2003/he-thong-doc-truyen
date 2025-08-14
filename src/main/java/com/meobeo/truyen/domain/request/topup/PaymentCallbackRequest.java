package com.meobeo.truyen.domain.request.topup;

import lombok.Data;

import java.util.Map;

@Data
public class PaymentCallbackRequest {

    private Map<String, String> vnpayResponse;
}
