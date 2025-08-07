package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Tìm refresh token theo token string
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Tìm refresh token theo userId
     */
    Optional<RefreshToken> findByUserId(Long userId);

    /**
     * Xóa refresh token theo userId
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Xóa tất cả refresh token đã hết hạn
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Kiểm tra token có tồn tại và chưa hết hạn
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END FROM RefreshToken rt WHERE rt.token = :token AND rt.expiryDate > :now")
    boolean existsByTokenAndNotExpired(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * Upsert refresh token - cập nhật nếu tồn tại, tạo mới nếu chưa có
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO refresh_tokens (user_id, token, expiry_date) " +
            "VALUES (:userId, :token, :expiryDate) " +
            "ON CONFLICT (user_id) DO UPDATE SET " +
            "token = EXCLUDED.token, " +
            "expiry_date = EXCLUDED.expiry_date", nativeQuery = true)
    void upsertRefreshToken(@Param("userId") Long userId,
            @Param("token") String token,
            @Param("expiryDate") LocalDateTime expiryDate);

    /**
     * Atomic upsert refresh token với retry logic
     */
    @Transactional
    default void atomicUpsertRefreshToken(Long userId, String token, LocalDateTime expiryDate) {
        try {
            // Thử upsert trước
            upsertRefreshToken(userId, token, expiryDate);
        } catch (Exception e) {
            // Nếu upsert thất bại, thử delete và insert
            try {
                deleteByUserId(userId);
                // Tạo entity mới
                RefreshToken newToken = new RefreshToken();
                newToken.setUserId(userId);
                newToken.setToken(token);
                newToken.setExpiryDate(expiryDate);
                save(newToken);
            } catch (Exception retryException) {
                throw new RuntimeException("Không thể lưu refresh token sau retry", retryException);
            }
        }
    }
}