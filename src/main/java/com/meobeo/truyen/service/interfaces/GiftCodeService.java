package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.giftcode.CreateGiftCodeRequest;
import com.meobeo.truyen.domain.request.giftcode.UpdateGiftCodeRequest;
import com.meobeo.truyen.domain.request.giftcode.UseGiftCodeRequest;
import com.meobeo.truyen.domain.response.giftcode.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GiftCodeService {

    /**
     * Tạo gift code mới (ADMIN)
     */
    GiftCodeResponse createGiftCode(CreateGiftCodeRequest request);

    /**
     * Cập nhật gift code (ADMIN)
     */
    GiftCodeResponse updateGiftCode(Long id, UpdateGiftCodeRequest request);

    /**
     * Xóa gift code (ADMIN)
     */
    void deleteGiftCode(Long id);

    /**
     * Lấy tất cả gift codes đang active (ADMIN)
     */
    GiftCodeListResponse getAllActiveGiftCodes(Pageable pageable);

    /**
     * Lấy gift code theo ID (ADMIN)
     */
    GiftCodeResponse getGiftCodeById(Long id);

    /**
     * Sử dụng gift code (USER)
     */
    GiftCodeUsageResponse useGiftCode(UseGiftCodeRequest request, Long userId);

    /**
     * Lấy lịch sử sử dụng gift code (ADMIN)
     */
    GiftCodeUsageListResponse getGiftCodeUsageHistory(Long giftCodeId, Pageable pageable);

    /**
     * Kiểm tra gift code có hợp lệ không
     */
    boolean isValidGiftCode(String code, Long userId);
}
