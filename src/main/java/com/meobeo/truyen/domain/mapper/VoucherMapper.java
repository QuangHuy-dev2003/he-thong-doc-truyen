package com.meobeo.truyen.domain.mapper;

import com.meobeo.truyen.domain.entity.Voucher;
import com.meobeo.truyen.domain.entity.VoucherUsage;
import com.meobeo.truyen.domain.request.voucher.CreateVoucherRequest;
import com.meobeo.truyen.domain.request.voucher.UpdateVoucherRequest;
import com.meobeo.truyen.domain.response.voucher.VoucherResponse;
import com.meobeo.truyen.domain.response.voucher.VoucherUsageResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class VoucherMapper {

    /**
     * Chuyển đổi CreateVoucherRequest thành Voucher entity
     */
    public Voucher toEntity(CreateVoucherRequest request) {
        Voucher voucher = new Voucher();
        voucher.setCode(request.getCode());
        voucher.setName(request.getName());
        voucher.setType(request.getType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinAmount(request.getMinAmount());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setMaxUsageCount(request.getMaxUsageCount());
        voucher.setMaxUsersCount(request.getMaxUsersCount());
        voucher.setMaxUsagePerUser(request.getMaxUsagePerUser());
        voucher.setValidFrom(request.getValidFrom());
        voucher.setValidUntil(request.getValidUntil());
        voucher.setDescription(request.getDescription());
        return voucher;
    }

    /**
     * Cập nhật Voucher entity từ UpdateVoucherRequest
     */
    public void updateEntityFromRequest(Voucher voucher, UpdateVoucherRequest request) {
        voucher.setName(request.getName());
        voucher.setType(request.getType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinAmount(request.getMinAmount());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setStatus(request.getStatus());
        voucher.setMaxUsageCount(request.getMaxUsageCount());
        voucher.setMaxUsersCount(request.getMaxUsersCount());
        voucher.setMaxUsagePerUser(request.getMaxUsagePerUser());
        voucher.setValidFrom(request.getValidFrom());
        voucher.setValidUntil(request.getValidUntil());
        voucher.setDescription(request.getDescription());
    }

    /**
     * Chuyển đổi Voucher entity thành VoucherResponse
     */
    public VoucherResponse toResponse(Voucher voucher) {
        VoucherResponse response = new VoucherResponse();
        response.setId(voucher.getId());
        response.setCode(voucher.getCode());
        response.setName(voucher.getName());
        response.setType(voucher.getType());
        response.setDiscountValue(voucher.getDiscountValue());
        response.setMinAmount(voucher.getMinAmount());
        response.setMaxDiscountAmount(voucher.getMaxDiscountAmount());
        response.setStatus(voucher.getStatus());
        response.setMaxUsageCount(voucher.getMaxUsageCount());
        response.setMaxUsersCount(voucher.getMaxUsersCount());
        response.setMaxUsagePerUser(voucher.getMaxUsagePerUser());
        response.setValidFrom(voucher.getValidFrom());
        response.setValidUntil(voucher.getValidUntil());
        response.setDescription(voucher.getDescription());
        response.setCreatedAt(voucher.getCreatedAt());
        return response;
    }

    /**
     * Chuyển đổi danh sách Voucher thành danh sách VoucherResponse
     */
    public List<VoucherResponse> toResponseList(List<Voucher> vouchers) {
        return vouchers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi VoucherUsage entity thành VoucherUsageResponse
     */
    public VoucherUsageResponse toUsageResponse(VoucherUsage voucherUsage) {
        VoucherUsageResponse response = new VoucherUsageResponse();
        response.setVoucherId(voucherUsage.getVoucher().getId());
        response.setVoucherCode(voucherUsage.getVoucher().getCode());
        response.setVoucherName(voucherUsage.getVoucher().getName());
        response.setUserId(voucherUsage.getUser().getId());
        response.setUserName(voucherUsage.getUser().getUsername());
        response.setOriginalAmount(voucherUsage.getOriginalAmount());
        response.setDiscountAmount(voucherUsage.getDiscountAmount());
        response.setFinalAmount(voucherUsage.getOriginalAmount().subtract(voucherUsage.getDiscountAmount()));
        response.setUsedAt(voucherUsage.getUsedAt());
        return response;
    }

    /**
     * Chuyển đổi danh sách VoucherUsage thành danh sách VoucherUsageResponse
     */
    public List<VoucherUsageResponse> toUsageResponseList(List<VoucherUsage> voucherUsages) {
        return voucherUsages.stream()
                .map(this::toUsageResponse)
                .collect(Collectors.toList());
    }
}
