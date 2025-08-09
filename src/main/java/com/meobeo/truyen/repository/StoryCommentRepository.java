package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.StoryComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryCommentRepository extends JpaRepository<StoryComment, Long> {

    /**
     * Lấy danh sách comment của story với phân trang
     * Order theo thời gian tạo mới nhất
     */
    @Query("SELECT sc FROM StoryComment sc " +
            "JOIN FETCH sc.user u " +
            "WHERE sc.story.id = :storyId " +
            "ORDER BY sc.createdAt DESC")
    Page<StoryComment> findByStoryIdOrderByCreatedAtDesc(
            @Param("storyId") Long storyId,
            Pageable pageable);

    /**
     * Đếm số comment của story
     */
    @Query("SELECT COUNT(sc) FROM StoryComment sc " +
            "WHERE sc.story.id = :storyId")
    Long countByStoryId(@Param("storyId") Long storyId);

    /**
     * Lấy danh sách comment của user trong story để kiểm tra spam
     */
    @Query("SELECT sc FROM StoryComment sc " +
            "WHERE sc.user.id = :userId AND sc.story.id = :storyId " +
            "ORDER BY sc.createdAt DESC")
    Page<StoryComment> findByUserIdAndStoryIdOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("storyId") Long storyId,
            Pageable pageable);

    /**
     * Kiểm tra xem user đã comment trong story này trong thời gian gần đây chưa
     * (chống spam) - đếm trong toàn bộ story
     */
    @Query("SELECT COUNT(sc) FROM StoryComment sc " +
            "WHERE sc.user.id = :userId AND sc.story.id = :storyId " +
            "AND sc.createdAt >= :sinceTime")
    Long countRecentCommentsByUserAndStory(
            @Param("userId") Long userId,
            @Param("storyId") Long storyId,
            @Param("sinceTime") java.time.LocalDateTime sinceTime);

    /**
     * Kiểm tra số lần user update comment gần đây (rate limiting cho update)
     */
    @Query("SELECT COUNT(sc) FROM StoryComment sc " +
            "WHERE sc.user.id = :userId AND sc.updatedAt >= :sinceTime " +
            "AND sc.updatedAt IS NOT NULL")
    Long countRecentUpdatesByUser(
            @Param("userId") Long userId,
            @Param("sinceTime") java.time.LocalDateTime sinceTime);

    /**
     * Tìm comment theo ID với thông tin user và story
     */
    @Query("SELECT sc FROM StoryComment sc " +
            "JOIN FETCH sc.user u " +
            "JOIN FETCH sc.story s " +
            "WHERE sc.id = :commentId")
    java.util.Optional<StoryComment> findByIdWithDetails(@Param("commentId") Long commentId);
}
