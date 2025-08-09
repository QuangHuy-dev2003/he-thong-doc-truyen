package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.enums.StoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

        /**
         * Tìm truyện theo slug
         */
        Optional<Story> findBySlug(String slug);

        /**
         * Tìm truyện theo ID với JOIN FETCH để tránh lazy loading issues
         */
        @Query("SELECT DISTINCT s FROM Story s " +
                        "LEFT JOIN FETCH s.author " +
                        "LEFT JOIN FETCH s.genres " +
                        "WHERE s.id = :storyId")
        Optional<Story> findByIdWithFetch(@Param("storyId") Long storyId);

        /**
         * Tìm truyện theo slug với JOIN FETCH để tránh lazy loading issues
         */
        @Query("SELECT DISTINCT s FROM Story s " +
                        "LEFT JOIN FETCH s.author " +
                        "LEFT JOIN FETCH s.genres " +
                        "WHERE s.slug = :slug")
        Optional<Story> findBySlugWithFetch(@Param("slug") String slug);

        /**
         * Kiểm tra slug đã tồn tại chưa
         */
        boolean existsBySlug(String slug);

        /**
         * Kiểm tra slug đã tồn tại chưa (trừ story hiện tại)
         */
        boolean existsBySlugAndIdNot(String slug, Long id);

        /**
         * Tìm truyện theo tác giả
         */
        Page<Story> findByAuthorId(Long authorId, Pageable pageable);

        /**
         * Tìm truyện theo tác giả với JOIN FETCH để tránh lazy loading issues
         */
        @Query("SELECT DISTINCT s FROM Story s " +
                        "LEFT JOIN FETCH s.author " +
                        "LEFT JOIN FETCH s.genres " +
                        "WHERE s.author.id = :authorId")
        List<Story> findByAuthorIdWithFetch(@Param("authorId") Long authorId);

        /**
         * Tìm kiếm truyện theo title, slug hoặc tên tác giả
         */
        @Query("SELECT s FROM Story s " +
                        "LEFT JOIN s.author a " +
                        "WHERE LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(s.slug) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(a.displayName) LIKE LOWER(CONCAT('%', :search, '%'))")
        Page<Story> searchStories(@Param("search") String search, Pageable pageable);

        /**
         * Tìm truyện theo thể loại
         */
        @Query("SELECT DISTINCT s FROM Story s " +
                        "JOIN s.genres g " +
                        "WHERE g.id IN :genreIds")
        Page<Story> findByGenreIds(@Param("genreIds") List<Long> genreIds, Pageable pageable);

        /**
         * Tìm truyện theo trạng thái
         */
        Page<Story> findByStatus(StoryStatus status, Pageable pageable);

        /**
         * Tìm kiếm và lọc truyện phức tạp - tối ưu cho PostgreSQL
         */
        @Query(value = "SELECT DISTINCT s.* FROM stories s " +
                        "LEFT JOIN users a ON s.author_id = a.id " +
                        "LEFT JOIN story_genres sg ON s.id = sg.story_id " +
                        "WHERE (:search IS NULL OR :search = '' OR " +
                        "s.title ILIKE CONCAT('%', :search, '%') OR " +
                        "s.slug ILIKE CONCAT('%', :search, '%') OR " +
                        "s.author_name ILIKE CONCAT('%', :search, '%') OR " +
                        "a.username ILIKE CONCAT('%', :search, '%') OR " +
                        "a.display_name ILIKE CONCAT('%', :search, '%')) " +
                        "AND (:status IS NULL OR s.status = CAST(:status AS VARCHAR)) " +
                        "AND (:genreIdsSize = 0 OR sg.genre_id = ANY(CAST(:genreIdsArray AS BIGINT[]))) " +
                        "ORDER BY s.created_at DESC", countQuery = "SELECT COUNT(DISTINCT s.id) FROM stories s " +
                                        "LEFT JOIN users a ON s.author_id = a.id " +
                                        "LEFT JOIN story_genres sg ON s.id = sg.story_id " +
                                        "WHERE (:search IS NULL OR :search = '' OR " +
                                        "s.title ILIKE CONCAT('%', :search, '%') OR " +
                                        "s.slug ILIKE CONCAT('%', :search, '%') OR " +
                                        "s.author_name ILIKE CONCAT('%', :search, '%') OR " +
                                        "a.username ILIKE CONCAT('%', :search, '%') OR " +
                                        "a.display_name ILIKE CONCAT('%', :search, '%')) " +
                                        "AND (:status IS NULL OR s.status = CAST(:status AS VARCHAR)) " +
                                        "AND (:genreIdsSize = 0 OR sg.genre_id = ANY(CAST(:genreIdsArray AS BIGINT[])))", nativeQuery = true)
        Page<Story> searchAndFilterStories(
                        @Param("search") String search,
                        @Param("status") String status,
                        @Param("genreIdsArray") Long[] genreIdsArray,
                        @Param("genreIdsSize") int genreIdsSize,
                        Pageable pageable);

        /**
         * Đếm số chapter của truyện
         */
        @Query("SELECT COUNT(c) FROM Chapter c WHERE c.story.id = :storyId")
        Long countChaptersByStoryId(@Param("storyId") Long storyId);

        /**
         * Đếm số view của truyện
         */
        @Query("SELECT COALESCE(SUM(svd.views), 0) FROM StoryViewsDaily svd WHERE svd.story.id = :storyId")
        Long countViewsByStoryId(@Param("storyId") Long storyId);

        /**
         * Đếm số favorite của truyện
         */
        @Query("SELECT COUNT(f) FROM Favorite f WHERE f.story.id = :storyId")
        Long countFavoritesByStoryId(@Param("storyId") Long storyId);

        /**
         * Đếm số vote của truyện
         */
        @Query("SELECT COUNT(v) FROM Vote v WHERE v.story.id = :storyId")
        Long countVotesByStoryId(@Param("storyId") Long storyId);

        /**
         * Tính điểm trung bình của truyện
         */
        @Query("SELECT COALESCE(AVG(v.rating), 0.0) FROM Vote v WHERE v.story.id = :storyId")
        Double getAverageRatingByStoryId(@Param("storyId") Long storyId);
}