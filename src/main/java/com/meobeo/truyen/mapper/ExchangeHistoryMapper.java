package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.WalletTransaction;
import com.meobeo.truyen.domain.response.spiritstone.ExchangeHistoryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExchangeHistoryMapper {

    /**
     * Chuyển WalletTransaction thành ExchangeHistoryResponse
     */
    public ExchangeHistoryResponse toResponse(WalletTransaction transaction) {
        ExchangeHistoryResponse response = new ExchangeHistoryResponse();
        response.setId(transaction.getId());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency().name());
        response.setType(transaction.getType().name());
        response.setDescription(transaction.getDescription());
        response.setCreatedAt(transaction.getCreatedAt());
        return response;
    }

    /**
     * Chuyển danh sách WalletTransaction thành danh sách ExchangeHistoryResponse
     */
    public List<ExchangeHistoryResponse> toResponseList(List<WalletTransaction> transactions) {
        return transactions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
