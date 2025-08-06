package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

        @Query("SELECT e FROM EmailOtp e WHERE e.email = :email AND e.userId = :userId AND e.isUsed = false AND e.expiresAt > :now ORDER BY e.createdAt DESC")
        Optional<EmailOtp> findLatestValidOtpByEmailAndUserId(@Param("email") String email,
                        @Param("userId") Long userId,
                        @Param("now") LocalDateTime now);

        @Query("SELECT e FROM EmailOtp e WHERE e.email = :email AND e.otpCode = :otpCode AND e.isUsed = false AND e.expiresAt > :now")
        Optional<EmailOtp> findValidOtpByEmailAndCode(@Param("email") String email,
                        @Param("otpCode") String otpCode,
                        @Param("now") LocalDateTime now);

        @Query("SELECT e FROM EmailOtp e WHERE e.userId = :userId AND e.isUsed = false AND e.expiresAt > :now")
        List<EmailOtp> findValidOtpsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

        @Query("SELECT e FROM EmailOtp e WHERE e.userId = :userId AND e.isUsed = false")
        List<EmailOtp> findUnusedOtpsByUserId(@Param("userId") Long userId);

        @Query("SELECT e FROM EmailOtp e WHERE e.email = :email AND e.isUsed = false")
        List<EmailOtp> findUnusedOtpsByEmail(@Param("email") String email);

        @Modifying
        @Transactional
        @Query("DELETE FROM EmailOtp e WHERE e.email = :email AND e.isUsed = false")
        void deleteUnusedOtpsByEmail(@Param("email") String email);
}