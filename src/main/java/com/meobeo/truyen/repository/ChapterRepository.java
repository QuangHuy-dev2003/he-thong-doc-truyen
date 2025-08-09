package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    /**
     * Lấy 5 chapter mới nhất của truyện
     */
    List<Chapter> findTop5ByStoryIdOrderByChapterNumberDesc(Long storyId);

    /**
     * Tìm chapter theo ID với thông tin story
     */
    @Query("SELECT c FROM Chapter c JOIN FETCH c.story WHERE c.id = :id")
    Optional<Chapter> findByIdWithStory(@Param("id") Long id);

    /**
     * Tìm chapter theo slug với thông tin story
     */
    @Query("SELECT c FROM Chapter c JOIN FETCH c.story WHERE c.slug = :slug")
    Optional<Chapter> findBySlugWithStory(@Param("slug") String slug);

    /**
     * Lấy danh sách chapter của truyện theo story ID với phân trang
     */
    @Query("SELECT c FROM Chapter c WHERE c.story.id = :storyId ORDER BY c.chapterNumber ASC")
    Page<Chapter> findByStoryIdOrderByChapterNumber(@Param("storyId") Long storyId, Pageable pageable);

    /**
     * Lấy danh sách chapter của truyện theo story slug với phân trang
     */
    @Query("SELECT c FROM Chapter c WHERE c.story.slug = :storySlug ORDER BY c.chapterNumber ASC")
    Page<Chapter> findByStorySlugOrderByChapterNumber(@Param("storySlug") String storySlug, Pageable pageable);

    /**
     * Kiểm tra slug chapter đã tồn tại chưa
     */
    boolean existsBySlug(String slug);

    /**
     * Kiểm tra slug chapter đã tồn tại chưa (trừ chapter hiện tại)
     */
    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * Kiểm tra chapter number đã tồn tại trong truyện chưa
     */
    boolean existsByStoryIdAndChapterNumber(Long storyId, Integer chapterNumber);

    /**
     * Kiểm tra chapter number đã tồn tại trong truyện chưa (trừ chapter hiện tại)
     */
    boolean existsByStoryIdAndChapterNumberAndIdNot(Long storyId, Integer chapterNumber, Long id);

    /**
     * Lấy chapter tiếp theo trong truyện
     */
    @Query("SELECT c FROM Chapter c WHERE c.story.id = :storyId AND c.chapterNumber > :currentChapterNumber ORDER BY c.chapterNumber ASC")
    Optional<Chapter> findNextChapter(@Param("storyId") Long storyId,
            @Param("currentChapterNumber") Integer currentChapterNumber);

    /**
     * Lấy chapter trước đó trong truyện
     */
    @Query("SELECT c FROM Chapter c WHERE c.story.id = :storyId AND c.chapterNumber < :currentChapterNumber ORDER BY c.chapterNumber DESC")
    Optional<Chapter> findPreviousChapter(@Param("storyId") Long storyId,
            @Param("currentChapterNumber") Integer currentChapterNumber);

    /**
     * Đếm số chapter của truyện
     */
    Long countByStoryId(Long storyId);

    /**
     * Lấy chapter số mới nhất của truyện
     */
    @Query("SELECT MAX(c.chapterNumber) FROM Chapter c WHERE c.story.id = :storyId")
    Optional<Integer> findMaxChapterNumberByStoryId(@Param("storyId") Long storyId);
}