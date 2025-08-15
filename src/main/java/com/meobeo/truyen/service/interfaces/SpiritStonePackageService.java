package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.entity.SpiritStonePackage;
import com.meobeo.truyen.domain.entity.WalletTransaction;
import com.meobeo.truyen.domain.request.spiritstone.CreateSpiritStonePackageRequest;
import com.meobeo.truyen.domain.request.spiritstone.ExchangeSpiritStoneByAmountRequest;
import com.meobeo.truyen.domain.request.spiritstone.ExchangeSpiritStoneByPackageRequest;
import com.meobeo.truyen.domain.request.spiritstone.UpdateSpiritStonePackageRequest;
import com.meobeo.truyen.domain.response.spiritstone.ExchangeHistoryListResponse;
import com.meobeo.truyen.domain.response.spiritstone.SpiritStonePackageListResponse;
import com.meobeo.truyen.domain.response.spiritstone.SpiritStonePackageResponse;
import com.meobeo.truyen.domain.response.spiritstone.WalletBalanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SpiritStonePackageService {

    // ADMIN functions
    /**
     * Tạo gói mới
     */
    SpiritStonePackageResponse createSpiritStonePackage(CreateSpiritStonePackageRequest request);

    /**
     * Cập nhật gói
     */
    SpiritStonePackageResponse updateSpiritStonePackage(Long id, UpdateSpiritStonePackageRequest request);

    /**
     * Xóa gói (set isActive = false)
     */
    void deleteSpiritStonePackage(Long id);

    // USER functions
    /**
     * Lấy tất cả gói đang hoạt động
     */
    SpiritStonePackageListResponse getAllActivePackages();

    /**
     * Lấy chi tiết gói theo ID
     */
    SpiritStonePackageResponse getPackageById(Long id);

    /**
     * Thực hiện đổi linh thạch theo gói
     */
    WalletBalanceResponse exchangeSpiritStoneByPackage(ExchangeSpiritStoneByPackageRequest request, Long userId);

    /**
     * Thực hiện đổi linh thạch theo số tiền
     */
    WalletBalanceResponse exchangeSpiritStoneByAmount(ExchangeSpiritStoneByAmountRequest request, Long userId);

    /**
     * Lịch sử đổi linh thạch (từ WalletTransaction)
     */
    ExchangeHistoryListResponse getExchangeHistory(Long userId, Pageable pageable);
}
