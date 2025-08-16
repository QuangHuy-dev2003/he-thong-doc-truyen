package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.ChapterPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterPaymentRepository extends JpaRepository<ChapterPayment, Long> {

        /**
         * Tìm payment info theo chapter ID
         */
        Optional<ChapterPayment> findByChapterId(Long chapterId);

        /**
         * Tìm payment info theo chapter ID với thông tin chapter và story
         */
        @Query("SELECT cp FROM ChapterPayment cp " +
                        "JOIN FETCH cp.chapter c " +
                        "JOIN FETCH cp.story s " +
                        "WHERE cp.chapterId = :chapterId")
        Optional<ChapterPayment> findByChapterIdWithDetails(@Param("chapterId") Long chapterId);

        /**
         * Lấy danh sách chapter payments của một story (có phân trang)
         * Sử dụng native query để tránh conflict với Spring Data JPA sorting
         */
        @Query(value = "SELECT cp.* FROM chapter_payments cp " +
                        "INNER JOIN chapters c ON cp.chapter_id = c.id " +
                        "INNER JOIN stories s ON cp.story_id = s.id " +
                        "WHERE cp.story_id = :storyId " +
                        "ORDER BY c.chapter_number ASC", nativeQuery = true, countQuery = "SELECT COUNT(*) FROM chapter_payments cp "
                                        +
                                        "WHERE cp.story_id = :storyId")
        Page<ChapterPayment> findByStoryIdOrderByChapterNumber(@Param("storyId") Long storyId, Pageable pageable);

        /**
         * Lấy danh sách chapter payments của một story (không phân trang)
         */
        @Query("SELECT cp FROM ChapterPayment cp " +
                        "WHERE cp.storyId = :storyId " +
                        "ORDER BY cp.chapter.chapterNumber ASC")
        List<ChapterPayment> findByStoryIdOrderByChapterNumber(@Param("storyId") Long storyId);

        /**
         * Kiểm tra chapter có bị khóa không
         */
        @Query("SELECT cp.isLocked FROM ChapterPayment cp WHERE cp.chapterId = :chapterId")
        Optional<Boolean> isChapterLocked(@Param("chapterId") Long chapterId);

        /**
         * Lấy giá của chapter
         */
        @Query("SELECT cp.price FROM ChapterPayment cp WHERE cp.chapterId = :chapterId")
        Optional<Integer> getChapterPrice(@Param("chapterId") Long chapterId);

        /**
         * Đếm số chapter bị khóa trong story
         */
        @Query("SELECT COUNT(cp) FROM ChapterPayment cp " +
                        "WHERE cp.storyId = :storyId AND cp.isLocked = true")
        Long countLockedChaptersByStory(@Param("storyId") Long storyId);

        /**
         * Kiểm tra xem có payment setting cho chapter hay chưa
         */
        boolean existsByChapterId(Long chapterId);

        /**
         * Update payment setting trực tiếp bằng query để tránh OptimisticLocking
         */
        @Modifying
        @Query("UPDATE ChapterPayment cp SET cp.price = :price, cp.isVipOnly = :isVipOnly, cp.isLocked = :isLocked " +
                        "WHERE cp.chapterId = :chapterId")
        int updateChapterPayment(@Param("chapterId") Long chapterId,
                        @Param("price") Integer price,
                        @Param("isVipOnly") Boolean isVipOnly,
                        @Param("isLocked") Boolean isLocked);

        /**
         * Insert payment setting mới bằng native SQL để tránh transaction conflict
         */
        @Modifying
        @Query(value = "INSERT INTO chapter_payments (chapter_id, story_id, price, is_vip_only, is_locked) " +
                        "VALUES (:chapterId, :storyId, :price, :isVipOnly, :isLocked)", nativeQuery = true)
        int insertChapterPayment(@Param("chapterId") Long chapterId,
                        @Param("storyId") Long storyId,
                        @Param("price") Integer price,
                        @Param("isVipOnly") Boolean isVipOnly,
                        @Param("isLocked") Boolean isLocked);

        /**
         * Lấy danh sách chapterIds đã bị khóa trong batch để tối ưu việc check
         */
        @Query("SELECT cp.chapterId FROM ChapterPayment cp WHERE cp.chapterId IN :chapterIds")
        List<Long> findLockedChapterIds(@Param("chapterIds") List<Long> chapterIds);

        /**
         * Insert ChapterPayment với ON CONFLICT DO NOTHING để tránh duplicate key
         * exception
         */
        @Modifying
        @Query(value = "INSERT INTO chapter_payments (chapter_id, story_id, price, is_vip_only, is_locked) " +
                        "VALUES (:chapterId, :storyId, :price, :isVipOnly, :isLocked) " +
                        "ON CONFLICT (chapter_id) DO NOTHING", nativeQuery = true)
        int insertChapterPaymentIgnoreDuplicate(@Param("chapterId") Long chapterId,
                        @Param("storyId") Long storyId,
                        @Param("price") Integer price,
                        @Param("isVipOnly") Boolean isVipOnly,
                        @Param("isLocked") Boolean isLocked);

        /**
         * Lấy danh sách chapter bị khóa theo story với phân trang
         * Tối ưu cho việc check unlock full story
         */
        @Query(value = "SELECT cp.chapter_id, cp.price, c.chapter_number, c.title " +
                        "FROM chapter_payments cp " +
                        "INNER JOIN chapters c ON cp.chapter_id = c.id " +
                        "WHERE cp.story_id = :storyId AND cp.is_locked = true " +
                        "ORDER BY c.chapter_number ASC " +
                        "LIMIT :limit OFFSET :offset", nativeQuery = true)
        List<Object[]> findLockedChaptersByStoryWithPagination(
                        @Param("storyId") Long storyId,
                        @Param("limit") int limit,
                        @Param("offset") int offset);

        /**
         * Lấy danh sách chapter đã unlock của user trong story
         * Tối ưu cho việc check trạng thái unlock
         */
        @Query("SELECT cu.chapter.id FROM ChapterUnlock cu " +
                        "WHERE cu.user.id = :userId AND cu.chapter.story.id = :storyId")
        List<Long> findUnlockedChapterIdsByUserAndStory(@Param("userId") Long userId, @Param("storyId") Long storyId);

        /**
         * Lấy danh sách chapter cần unlock (bị khóa và chưa unlock)
         * Tối ưu query với JOIN để tránh N+1
         */
        @Query(value = "SELECT cp.chapter_id, cp.price, c.chapter_number, c.title " +
                        "FROM chapter_payments cp " +
                        "INNER JOIN chapters c ON cp.chapter_id = c.id " +
                        "WHERE cp.story_id = :storyId " +
                        "AND cp.is_locked = true " +
                        "AND cp.chapter_id NOT IN (" +
                        "    SELECT cu.chapter_id FROM chapter_unlocks cu " +
                        "    WHERE cu.user_id = :userId" +
                        ") " +
                        "ORDER BY c.chapter_number ASC " +
                        "LIMIT :limit OFFSET :offset", nativeQuery = true)
        List<Object[]> findChaptersToUnlockByStory(
                        @Param("storyId") Long storyId,
                        @Param("userId") Long userId,
                        @Param("limit") int limit,
                        @Param("offset") int offset);

        /**
         * Đếm số chapter cần unlock (bị khóa và chưa unlock)
         */
        @Query(value = "SELECT COUNT(cp.chapter_id) " +
                        "FROM chapter_payments cp " +
                        "WHERE cp.story_id = :storyId " +
                        "AND cp.is_locked = true " +
                        "AND cp.chapter_id NOT IN (" +
                        "    SELECT cu.chapter_id FROM chapter_unlocks cu " +
                        "    WHERE cu.user_id = :userId" +
                        ")", nativeQuery = true)
        Long countChaptersToUnlockByStory(@Param("storyId") Long storyId, @Param("userId") Long userId);
}
