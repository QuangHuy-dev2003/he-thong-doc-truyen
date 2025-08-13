package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.ReadingHistory;
import com.meobeo.truyen.domain.entity.ReadingHistoryId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, ReadingHistoryId> {

    /**
     * Kiểm tra user đã đọc story chưa
     */
    boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Lấy reading history theo user và story
     */
    Optional<ReadingHistory> findByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Lấy danh sách reading history của user với phân trang
     */
    @Query("SELECT rh FROM ReadingHistory rh " +
            "LEFT JOIN FETCH rh.chapter c " +
            "LEFT JOIN FETCH rh.story s " +
            "LEFT JOIN FETCH s.author " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE rh.user.id = :userId " +
            "ORDER BY rh.lastReadAt DESC")
    Page<ReadingHistory> findByUserIdWithFetch(@Param("userId") Long userId, Pageable pageable);

    /**
     * Lấy danh sách reading history của user (không phân trang)
     */
    @Query("SELECT rh FROM ReadingHistory rh " +
            "LEFT JOIN FETCH rh.chapter c " +
            "LEFT JOIN FETCH rh.story s " +
            "LEFT JOIN FETCH s.author " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE rh.user.id = :userId " +
            "ORDER BY rh.lastReadAt DESC")
    List<ReadingHistory> findByUserIdWithFetch(@Param("userId") Long userId);

    /**
     * Lấy reading history của user cho một story cụ thể
     */
    @Query("SELECT rh FROM ReadingHistory rh " +
            "LEFT JOIN FETCH rh.chapter c " +
            "LEFT JOIN FETCH rh.story s " +
            "LEFT JOIN FETCH s.author " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE rh.user.id = :userId AND rh.story.id = :storyId")
    Optional<ReadingHistory> findByUserIdAndStoryIdWithFetch(@Param("userId") Long userId,
            @Param("storyId") Long storyId);

    /**
     * Lấy chapter cuối cùng user đã đọc trong story
     */
    @Query("SELECT rh FROM ReadingHistory rh " +
            "LEFT JOIN FETCH rh.chapter c " +
            "LEFT JOIN FETCH rh.story s " +
            "LEFT JOIN FETCH s.author " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE rh.user.id = :userId AND rh.story.id = :storyId")
    Optional<ReadingHistory> findLastReadChapterByUserAndStory(@Param("userId") Long userId,
            @Param("storyId") Long storyId);

    /**
     * Đếm số story đã đọc của user
     */
    @Query("SELECT COUNT(rh) FROM ReadingHistory rh WHERE rh.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    /**
     * Xóa reading history theo user và story
     */
    @Modifying
    @Transactional
    void deleteByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Xóa tất cả reading history của user
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ReadingHistory rh WHERE rh.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    /**
     * Lấy danh sách reading history cũ (để cleanup)
     */
    @Query("SELECT rh FROM ReadingHistory rh WHERE rh.lastReadAt < :cutoffDate")
    List<ReadingHistory> findOldReadingHistory(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Lấy danh sách story đã đọc của user, sắp xếp theo thời gian đọc gần nhất
     */
    @Query("SELECT rh FROM ReadingHistory rh " +
            "LEFT JOIN FETCH rh.chapter c " +
            "LEFT JOIN FETCH rh.story s " +
            "LEFT JOIN FETCH s.author " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE rh.user.id = :userId " +
            "ORDER BY rh.lastReadAt DESC")
    List<ReadingHistory> findLastReadStoriesByUser(@Param("userId") Long userId);
}
