package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.WalletTransaction;
import com.meobeo.truyen.domain.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

        /**
         * Lấy lịch sử giao dịch của user theo thời gian
         */
        @Query("SELECT wt FROM WalletTransaction wt WHERE wt.user.id = :userId ORDER BY wt.createdAt DESC")
        Page<WalletTransaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

        /**
         * Lấy lịch sử giao dịch theo loại
         */
        @Query("SELECT wt FROM WalletTransaction wt WHERE wt.user.id = :userId AND wt.type = :type ORDER BY wt.createdAt DESC")
        List<WalletTransaction> findByUserIdAndTypeOrderByCreatedAtDesc(@Param("userId") Long userId,
                        @Param("type") TransactionType type);

        /**
         * Lấy lịch sử giao dịch trong khoảng thời gian
         */
        @Query("SELECT wt FROM WalletTransaction wt WHERE wt.user.id = :userId AND wt.createdAt BETWEEN :startDate AND :endDate ORDER BY wt.createdAt DESC")
        List<WalletTransaction> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Lấy lịch sử giao dịch theo user và danh sách loại giao dịch
         */
        @Query("SELECT wt FROM WalletTransaction wt WHERE wt.user = :user AND wt.type IN :types ORDER BY wt.createdAt DESC")
        Page<WalletTransaction> findByUserAndTypeInOrderByCreatedAtDesc(
                        @Param("user") com.meobeo.truyen.domain.entity.User user,
                        @Param("types") List<TransactionType> types, Pageable pageable);
}
