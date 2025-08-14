package com.meobeo.truyen.domain.repository;

import com.meobeo.truyen.domain.entity.SePayTopupRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SePayTopupRequestRepository extends JpaRepository<SePayTopupRequest, Long> {

    /**
     * Tìm theo transfer content (mã nội dung chuyển khoản)
     */
    Optional<SePayTopupRequest> findByTransferContent(String transferContent);

    /**
     * Tìm theo SePay transaction ID
     */
    Optional<SePayTopupRequest> findBySepayTransactionId(String sepayTransactionId);

    /**
     * Kiểm tra transfer content đã tồn tại chưa
     */
    boolean existsByTransferContent(String transferContent);

    /**
     * Lấy danh sách yêu cầu nạp tiền của user
     */
    @Query("SELECT s FROM SePayTopupRequest s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    Page<SePayTopupRequest> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * Lấy danh sách yêu cầu nạp tiền theo trạng thái
     */
    List<SePayTopupRequest> findByStatus(SePayTopupRequest.TopupStatus status);

    /**
     * Lấy danh sách yêu cầu nạp tiền pending để xử lý
     */
    @Query("SELECT s FROM SePayTopupRequest s WHERE s.status = 'PENDING' AND s.createdAt < :expireTime")
    List<SePayTopupRequest> findPendingRequestsBefore(@Param("expireTime") java.time.LocalDateTime expireTime);
}
