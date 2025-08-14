package com.meobeo.truyen.domain.repository;

import com.meobeo.truyen.domain.entity.Voucher;
import com.meobeo.truyen.domain.enums.VoucherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    /**
     * Tìm voucher theo mã và trạng thái
     */
    Optional<Voucher> findByCodeAndStatus(String code, VoucherStatus status);

    /**
     * Kiểm tra mã voucher đã tồn tại chưa
     */
    boolean existsByCode(String code);

    /**
     * Tìm tất cả voucher theo trạng thái
     */
    List<Voucher> findByStatus(VoucherStatus status);

    /**
     * Tìm tất cả voucher đang hoạt động
     */
    @Query("SELECT v FROM Voucher v WHERE v.status = :status AND v.validFrom <= :now AND v.validUntil >= :now")
    List<Voucher> findActiveVouchers(@Param("status") VoucherStatus status, @Param("now") LocalDateTime now);

    /**
     * Tìm voucher theo trạng thái với phân trang
     */
    Page<Voucher> findByStatus(VoucherStatus status, Pageable pageable);

    /**
     * Tìm voucher theo mã
     */
    Optional<Voucher> findByCode(String code);
}
