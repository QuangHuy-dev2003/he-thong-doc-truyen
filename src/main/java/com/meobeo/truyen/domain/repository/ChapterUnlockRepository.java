package com.meobeo.truyen.domain.repository;

import com.meobeo.truyen.domain.entity.ChapterUnlock;
import com.meobeo.truyen.domain.entity.ChapterUnlockId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterUnlockRepository extends JpaRepository<ChapterUnlock, ChapterUnlockId> {

        /**
         * Kiểm tra user đã mở khóa chapter chưa
         */
        boolean existsByUserIdAndChapterId(Long userId, Long chapterId);

        /**
         * Lấy danh sách chapter đã mở khóa của user trong story (không phân trang)
         */
        @Query("SELECT cu.chapter.id FROM ChapterUnlock cu " +
                        "WHERE cu.user.id = :userId AND cu.chapter.story.id = :storyId " +
                        "ORDER BY cu.chapter.chapterNumber")
        List<Long> findUnlockedChapterIdsByUserAndStory(@Param("userId") Long userId, @Param("storyId") Long storyId);

        /**
         * Lấy danh sách chapter đã mở khóa của user trong story (có phân trang)
         */
        @Query("SELECT cu.chapter.id FROM ChapterUnlock cu " +
                        "WHERE cu.user.id = :userId AND cu.chapter.story.id = :storyId " +
                        "ORDER BY cu.chapter.chapterNumber")
        Page<Long> findUnlockedChapterIdsByUserAndStory(@Param("userId") Long userId, @Param("storyId") Long storyId,
                        Pageable pageable);

        /**
         * Đếm số chapter đã mở khóa của user trong story
         */
        @Query("SELECT COUNT(cu) FROM ChapterUnlock cu " +
                        "WHERE cu.user.id = :userId AND cu.chapter.story.id = :storyId")
        long countUnlockedChaptersByUserAndStory(@Param("userId") Long userId, @Param("storyId") Long storyId);

        /**
         * Lấy danh sách ChapterUnlock của user trong story
         */
        @Query("SELECT cu FROM ChapterUnlock cu " +
                        "WHERE cu.user.id = :userId AND cu.chapter.story.id = :storyId " +
                        "ORDER BY cu.chapter.chapterNumber")
        List<ChapterUnlock> findByUserIdAndStoryId(@Param("userId") Long userId, @Param("storyId") Long storyId);
}
