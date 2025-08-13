package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.GiftCodeUsage;
import com.meobeo.truyen.domain.entity.GiftCodeUsageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GiftCodeUsageRepository extends JpaRepository<GiftCodeUsage, GiftCodeUsageId> {

    /**
     * Đếm số lượt sử dụng của một gift code
     */
    long countByGiftCodeId(Long giftCodeId);

    /**
     * Đếm số lượt sử dụng của một user với một gift code
     */
    long countByGiftCodeIdAndUserId(Long giftCodeId, Long userId);

    /**
     * Kiểm tra user đã sử dụng gift code chưa
     */
    boolean existsByGiftCodeIdAndUserId(Long giftCodeId, Long userId);

    /**
     * Lấy tất cả lịch sử sử dụng của một gift code
     */
    List<GiftCodeUsage> findByGiftCodeIdOrderByUsedAtDesc(Long giftCodeId);

    /**
     * Lấy lịch sử sử dụng gift code của một user
     */
    List<GiftCodeUsage> findByUserIdOrderByUsedAtDesc(Long userId);
}
