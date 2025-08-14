package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.PaymentTransaction;
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
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Tìm giao dịch theo orderId
     */
    Optional<PaymentTransaction> findByOrderId(String orderId);

    /**
     * Tìm giao dịch theo vnpayTransactionId
     */
    Optional<PaymentTransaction> findByVnpayTransactionId(String vnpayTransactionId);

    /**
     * Kiểm tra orderId đã tồn tại chưa
     */
    boolean existsByOrderId(String orderId);

    /**
     * Lấy lịch sử giao dịch của user
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.user.id = :userId ORDER BY pt.createdAt DESC")
    Page<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * Lấy giao dịch theo trạng thái
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.status = :status ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findByStatusOrderByCreatedAtDesc(@Param("status") PaymentTransaction.PaymentStatus status);

    /**
     * Lấy giao dịch của user theo trạng thái
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.user.id = :userId AND pt.status = :status ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findByUserIdAndStatusOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("status") PaymentTransaction.PaymentStatus status);

    /**
     * Lấy giao dịch trong khoảng thời gian
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.createdAt BETWEEN :startDate AND :endDate ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findByCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy giao dịch PENDING đã quá hạn (15 phút)
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.status = 'PENDING' AND pt.createdAt < :expiredTime")
    List<PaymentTransaction> findExpiredPendingTransactions(@Param("expiredTime") LocalDateTime expiredTime);

    /**
     * Đếm số giao dịch theo trạng thái
     */
    @Query("SELECT COUNT(pt) FROM PaymentTransaction pt WHERE pt.status = :status")
    long countByStatus(@Param("status") PaymentTransaction.PaymentStatus status);

    /**
     * Đếm số giao dịch thành công của user
     */
    @Query("SELECT COUNT(pt) FROM PaymentTransaction pt WHERE pt.user.id = :userId AND pt.status = 'SUCCESS'")
    long countSuccessfulTransactionsByUserId(@Param("userId") Long userId);
}
