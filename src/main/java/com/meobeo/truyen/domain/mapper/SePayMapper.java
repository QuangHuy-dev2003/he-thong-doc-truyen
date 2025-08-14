package com.meobeo.truyen.domain.mapper;

import com.meobeo.truyen.config.SePayConfig;
import com.meobeo.truyen.domain.entity.SePayTopupRequest;
import com.meobeo.truyen.domain.entity.TopupPackage;
import com.meobeo.truyen.domain.response.topup.SePayTopupHistoryResponse;
import com.meobeo.truyen.domain.response.topup.SePayTopupResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SePayMapper {

    private final SePayConfig sePayConfig;

    public SePayMapper(SePayConfig sePayConfig) {
        this.sePayConfig = sePayConfig;
    }

    /**
     * Chuyển đổi SePayTopupRequest entity thành SePayTopupResponse
     */
    public SePayTopupResponse toTopupResponse(SePayTopupRequest topupRequest, TopupPackage topupPackage) {
        SePayTopupResponse response = new SePayTopupResponse();
        response.setRequestId(topupRequest.getId());
        response.setAmount(topupRequest.getAmount());
        response.setOriginalAmount(topupRequest.getOriginalAmount());
        response.setDiscountAmount(topupRequest.getDiscountAmount());
        response.setVoucherCode(topupRequest.getVoucherCode());
        response.setPackageName(topupPackage.getName());
        response.setPackageId(topupPackage.getId());
        response.setTransferContent(topupRequest.getTransferContent());
        response.setAccountNumber(sePayConfig.getAccountNumber());
        response.setBankName(sePayConfig.getBankName());
        response.setCreatedAt(topupRequest.getCreatedAt());
        response.setExpiresAt(topupRequest.getCreatedAt().plusMinutes(sePayConfig.getTimeoutMinutes()));

        // Tạo QR URL
        String qrUrl = sePayConfig.buildQrUrl(
                topupRequest.getAmount().toString(),
                topupRequest.getTransferContent());
        response.setQrUrl(qrUrl);

        return response;
    }

    /**
     * Chuyển đổi SePayTopupRequest entity thành SePayTopupHistoryItem
     */
    public SePayTopupHistoryResponse.SePayTopupHistoryItem toHistoryItem(SePayTopupRequest topupRequest) {
        SePayTopupHistoryResponse.SePayTopupHistoryItem item = new SePayTopupHistoryResponse.SePayTopupHistoryItem();
        item.setRequestId(topupRequest.getId());
        item.setAmount(topupRequest.getAmount());
        item.setOriginalAmount(topupRequest.getOriginalAmount());
        item.setDiscountAmount(topupRequest.getDiscountAmount());
        item.setVoucherCode(topupRequest.getVoucherCode());
        item.setTransferContent(topupRequest.getTransferContent());
        item.setStatus(topupRequest.getStatus().name());
        item.setCreatedAt(topupRequest.getCreatedAt());
        item.setProcessedAt(topupRequest.getProcessedAt());
        return item;
    }

    /**
     * Chuyển đổi danh sách SePayTopupRequest thành danh sách SePayTopupHistoryItem
     */
    public List<SePayTopupHistoryResponse.SePayTopupHistoryItem> toHistoryItemList(
            List<SePayTopupRequest> topupRequests) {
        return topupRequests.stream()
                .map(this::toHistoryItem)
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi Page<SePayTopupRequest> thành SePayTopupHistoryResponse
     */
    public SePayTopupHistoryResponse toHistoryResponse(Page<SePayTopupRequest> page) {
        SePayTopupHistoryResponse response = new SePayTopupHistoryResponse();
        response.setContent(toHistoryItemList(page.getContent()));
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }
}
