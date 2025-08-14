package com.meobeo.truyen.domain.repository;

import com.meobeo.truyen.domain.entity.VoucherUsage;
import com.meobeo.truyen.domain.entity.VoucherUsageId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, VoucherUsageId> {

    /**
     * Đếm số lần sử dụng của một voucher
     */
    long countById_VoucherId(Long voucherId);

    /**
     * Đếm số lần sử dụng của một voucher bởi một user
     */
    long countById_VoucherIdAndId_UserId(Long voucherId, Long userId);

    /**
     * Kiểm tra user đã sử dụng voucher chưa
     */
    boolean existsById_VoucherIdAndId_UserId(Long voucherId, Long userId);

    /**
     * Tìm tất cả lịch sử sử dụng của một voucher
     */
    Page<VoucherUsage> findById_VoucherId(Long voucherId, Pageable pageable);

    /**
     * Tìm tất cả lịch sử sử dụng của một user
     */
    Page<VoucherUsage> findById_UserId(Long userId, Pageable pageable);

    /**
     * Đếm số user đã sử dụng voucher
     */
    @Query("SELECT COUNT(DISTINCT vu.user.id) FROM VoucherUsage vu WHERE vu.voucher.id = :voucherId")
    long countDistinctUsersByVoucherId(@Param("voucherId") Long voucherId);
}