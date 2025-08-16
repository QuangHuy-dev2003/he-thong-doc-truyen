package com.meobeo.truyen.domain.repository;

import com.meobeo.truyen.domain.entity.StoryRecommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryRecommendationRepository extends JpaRepository<StoryRecommendation, Long> {

    /**
     * Lấy danh sách đề cử của user theo thời gian tạo giảm dần
     */
    Page<StoryRecommendation> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Lấy danh sách đề cử của story theo thời gian tạo giảm dần
     */
    Page<StoryRecommendation> findByStoryIdOrderByCreatedAtDesc(Long storyId, Pageable pageable);

    /**
     * Đếm số đề cử của story
     */
    long countByStoryId(Long storyId);

    /**
     * Kiểm tra user đã đề cử story này chưa
     */
    boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Lấy top stories được đề cử nhiều nhất
     */
    @Query("SELECT sr.storyId, COUNT(sr) as recommendationCount " +
            "FROM StoryRecommendation sr " +
            "GROUP BY sr.storyId " +
            "ORDER BY recommendationCount DESC")
    Page<Object[]> findTopRecommendedStories(Pageable pageable);

    /**
     * Đếm số đề cử của user trong ngày
     */
    @Query(value = "SELECT COUNT(*) FROM story_recommendations sr " +
            "WHERE sr.user_id = :userId " +
            "AND DATE(sr.created_at) = CURRENT_DATE", nativeQuery = true)
    long countByUserIdAndCreatedAtToday(@Param("userId") Long userId);

    /**
     * Lấy danh sách story được đề cử nhiều nhất với số lượng đề cử
     */
    @Query("SELECT sr.storyId, COUNT(sr) as count " +
            "FROM StoryRecommendation sr " +
            "GROUP BY sr.storyId " +
            "ORDER BY count DESC")
    List<Object[]> findTopStoriesWithRecommendationCount(Pageable pageable);
}
