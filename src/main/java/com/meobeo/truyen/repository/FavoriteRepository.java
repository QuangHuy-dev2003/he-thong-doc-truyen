package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.Favorite;
import com.meobeo.truyen.domain.entity.FavoriteId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    /**
     * Kiểm tra user đã yêu thích truyện chưa
     */
    boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Lấy favorite theo user và story
     */
    Optional<Favorite> findByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Lấy danh sách favorite của user với phân trang
     */
    @Query("SELECT f FROM Favorite f " +
            "LEFT JOIN FETCH f.story s " +
            "LEFT JOIN FETCH s.author " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE f.user.id = :userId " +
            "ORDER BY f.favoritedAt DESC")
    Page<Favorite> findByUserIdWithFetch(@Param("userId") Long userId, Pageable pageable);

    /**
     * Lấy danh sách favorite của user (không phân trang)
     */
    @Query("SELECT f FROM Favorite f " +
            "LEFT JOIN FETCH f.story s " +
            "LEFT JOIN FETCH s.author " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE f.user.id = :userId " +
            "ORDER BY f.favoritedAt DESC")
    List<Favorite> findByUserIdWithFetch(@Param("userId") Long userId);

    /**
     * Đếm số favorite của user
     */
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    /**
     * Đếm số favorite của truyện
     */
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.story.id = :storyId")
    Long countByStoryId(@Param("storyId") Long storyId);

    /**
     * Xóa favorite theo user và story
     */
    void deleteByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Kiểm tra user có yêu thích truyện không
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Favorite f " +
            "WHERE f.user.id = :userId AND f.story.id = :storyId")
    boolean isUserFavoriteStory(@Param("userId") Long userId, @Param("storyId") Long storyId);
}
