package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.StorySubscription;
import com.meobeo.truyen.domain.entity.StorySubscriptionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorySubscriptionRepository extends JpaRepository<StorySubscription, StorySubscriptionId> {

    /**
     * Kiểm tra user đã đăng ký theo dõi truyện chưa
     */
    boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Lấy subscription theo user và story
     */
    Optional<StorySubscription> findByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Lấy danh sách subscription của user với phân trang
     */
    @Query("SELECT ss FROM StorySubscription ss " +
            "LEFT JOIN FETCH ss.story s " +
            "LEFT JOIN FETCH s.author " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE ss.user.id = :userId " +
            "ORDER BY ss.subscribedAt DESC")
    Page<StorySubscription> findByUserIdWithFetch(@Param("userId") Long userId, Pageable pageable);

    /**
     * Lấy danh sách subscription của user (không phân trang)
     */
    @Query("SELECT ss FROM StorySubscription ss " +
            "LEFT JOIN FETCH ss.story s " +
            "LEFT JOIN FETCH s.author " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE ss.user.id = :userId " +
            "ORDER BY ss.subscribedAt DESC")
    List<StorySubscription> findByUserIdWithFetch(@Param("userId") Long userId);

    /**
     * Đếm số subscription của user
     */
    @Query("SELECT COUNT(ss) FROM StorySubscription ss WHERE ss.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    /**
     * Đếm số người đăng ký theo dõi truyện
     */
    @Query("SELECT COUNT(ss) FROM StorySubscription ss WHERE ss.story.id = :storyId")
    Long countByStoryId(@Param("storyId") Long storyId);

    /**
     * Xóa subscription theo user và story
     */
    void deleteByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Kiểm tra user có đăng ký theo dõi truyện không
     */
    @Query("SELECT CASE WHEN COUNT(ss) > 0 THEN true ELSE false END FROM StorySubscription ss " +
            "WHERE ss.user.id = :userId AND ss.story.id = :storyId")
    boolean isUserSubscribedToStory(@Param("userId") Long userId, @Param("storyId") Long storyId);

    /**
     * Lấy danh sách user đăng ký theo dõi truyện
     */
    @Query("SELECT ss FROM StorySubscription ss " +
            "LEFT JOIN FETCH ss.user u " +
            "WHERE ss.story.id = :storyId " +
            "ORDER BY ss.subscribedAt DESC")
    List<StorySubscription> findByStoryIdWithUserFetch(@Param("storyId") Long storyId);
}
