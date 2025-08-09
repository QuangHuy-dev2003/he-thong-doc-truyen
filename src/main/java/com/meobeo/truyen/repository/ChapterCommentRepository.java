package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.ChapterComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterCommentRepository extends JpaRepository<ChapterComment, Long> {

    /**
     * Lấy danh sách comment của chapter theo storyId và chapterId với phân trang
     * Order theo thời gian tạo mới nhất
     */
    @Query("SELECT cc FROM ChapterComment cc " +
            "JOIN FETCH cc.user u " +
            "WHERE cc.story.id = :storyId AND cc.chapter.id = :chapterId " +
            "ORDER BY cc.createdAt DESC")
    Page<ChapterComment> findByStoryIdAndChapterIdOrderByCreatedAtDesc(
            @Param("storyId") Long storyId,
            @Param("chapterId") Long chapterId,
            Pageable pageable);

    /**
     * Lấy danh sách comment của chapter theo storyId và chapterNumber với phân
     * trang
     */
    @Query("SELECT cc FROM ChapterComment cc " +
            "JOIN FETCH cc.user u " +
            "JOIN cc.chapter c " +
            "WHERE cc.story.id = :storyId AND c.chapterNumber = :chapterNumber " +
            "ORDER BY cc.createdAt DESC")
    Page<ChapterComment> findByStoryIdAndChapterNumberOrderByCreatedAtDesc(
            @Param("storyId") Long storyId,
            @Param("chapterNumber") Integer chapterNumber,
            Pageable pageable);

    /**
     * Đếm số comment của chapter
     */
    @Query("SELECT COUNT(cc) FROM ChapterComment cc " +
            "WHERE cc.story.id = :storyId AND cc.chapter.id = :chapterId")
    Long countByStoryIdAndChapterId(@Param("storyId") Long storyId, @Param("chapterId") Long chapterId);

    /**
     * Đếm số comment của chapter theo chapterNumber
     */
    @Query("SELECT COUNT(cc) FROM ChapterComment cc " +
            "JOIN cc.chapter c " +
            "WHERE cc.story.id = :storyId AND c.chapterNumber = :chapterNumber")
    Long countByStoryIdAndChapterNumber(@Param("storyId") Long storyId, @Param("chapterNumber") Integer chapterNumber);

    /**
     * Lấy danh sách comment của user trong story để kiểm tra spam
     */
    @Query("SELECT cc FROM ChapterComment cc " +
            "WHERE cc.user.id = :userId AND cc.story.id = :storyId " +
            "ORDER BY cc.createdAt DESC")
    Page<ChapterComment> findByUserIdAndStoryIdOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("storyId") Long storyId,
            Pageable pageable);

    /**
     * Kiểm tra xem user đã comment trong chapter này trong thời gian gần đây chưa
     * (chống spam)
     */
    @Query("SELECT COUNT(cc) FROM ChapterComment cc " +
            "WHERE cc.user.id = :userId AND cc.chapter.id = :chapterId " +
            "AND cc.createdAt >= :sinceTime")
    Long countRecentCommentsByUserAndChapter(
            @Param("userId") Long userId,
            @Param("chapterId") Long chapterId,
            @Param("sinceTime") java.time.LocalDateTime sinceTime);
}
