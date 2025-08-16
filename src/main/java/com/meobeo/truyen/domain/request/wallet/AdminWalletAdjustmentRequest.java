package com.meobeo.truyen.domain.request.wallet;

import com.meobeo.truyen.domain.entity.WalletTransaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AdminWalletAdjustmentRequest {

    @NotNull(message = "User ID không được để trống")
    private Long userId;

    @NotNull(message = "Loại tiền tệ không được để trống")
    private WalletTransaction.CurrencyType currency;

    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phải lớn hơn 0")
    private Integer amount;

    @NotNull(message = "Loại điều chỉnh không được để trống")
    private AdjustmentType adjustmentType;

    private String description;

    public enum AdjustmentType {
        ADD, // Cộng tiền
        SUBTRACT // Trừ tiền
    }
}
