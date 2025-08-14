package com.meobeo.truyen.service;

import com.meobeo.truyen.domain.entity.TopupPackage;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.topup.CreateTopupPackageRequest;
import com.meobeo.truyen.domain.request.topup.TopupRequest;
import com.meobeo.truyen.domain.request.topup.UpdateTopupPackageRequest;
import com.meobeo.truyen.domain.response.topup.TopupPackageListResponse;
import com.meobeo.truyen.domain.response.topup.TopupPackageResponse;

import java.util.List;

public interface TopupPackageService {

    /**
     * Tạo gói nạp tiền mới (ADMIN)
     */
    TopupPackageResponse createTopupPackage(CreateTopupPackageRequest request);

    /**
     * Cập nhật gói nạp tiền (ADMIN)
     */
    TopupPackageResponse updateTopupPackage(Long id, UpdateTopupPackageRequest request);

    /**
     * Xóa gói nạp tiền (ADMIN)
     */
    void deleteTopupPackage(Long id);

    /**
     * Lấy tất cả gói nạp tiền đang hoạt động (Public)
     */
    TopupPackageListResponse getAllActivePackages();

    /**
     * Lấy gói nạp tiền theo ID (Public)
     */
    TopupPackageResponse getPackageById(Long id);

    /**
     * Xử lý nạp tiền và tạo transaction (USER)
     */
    void processTopup(TopupRequest request, User user);

    /**
     * Áp dụng voucher cho topup và tính toán giảm giá
     */
    java.math.BigDecimal applyVoucherToTopup(String voucherCode, java.math.BigDecimal originalAmount, Long userId);
}
