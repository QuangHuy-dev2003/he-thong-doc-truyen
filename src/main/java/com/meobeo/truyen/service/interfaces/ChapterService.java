package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.chapter.CreateChapterRequest;
import com.meobeo.truyen.domain.request.chapter.UpdateChapterRequest;
import com.meobeo.truyen.domain.response.chapter.ChapterResponse;
import com.meobeo.truyen.domain.response.chapter.ChapterListResponse;
import org.springframework.data.domain.Pageable;

public interface ChapterService {

    /**
     * Tạo chapter mới
     */
    ChapterResponse createChapter(CreateChapterRequest request, Long userId);

    /**
     * Cập nhật chapter theo storyId và chapterNumber
     */
    ChapterResponse updateChapterByStoryAndNumber(Long storyId, Integer chapterNumber, UpdateChapterRequest request,
            Long userId);

    /**
     * Xóa chapter
     */
    void deleteChapter(Long chapterId, Long userId);

    /**
     * Xóa chapter theo storyId và chapterNumber
     */
    void deleteChapterByStoryAndNumber(Long storyId, Integer chapterNumber, Long userId);

    /**
     * Lấy chi tiết chapter theo ID hoặc slug
     */
    ChapterResponse getChapterDetail(String identifier, Long userId);

    /**
     * Lấy chi tiết chapter theo storyId và chapterNumber
     */
    ChapterResponse getChapterDetailByStoryAndNumber(Long storyId, Integer chapterNumber, Long userId);

    /**
     * Lấy danh sách chapter của truyện
     */
    ChapterListResponse getChaptersByStory(String storyIdentifier, Pageable pageable);

    /**
     * Lấy chapter tiếp theo
     */
    ChapterResponse getNextChapter(Long chapterId);

    /**
     * Lấy chapter trước đó
     */
    ChapterResponse getPreviousChapter(Long chapterId);

    /**
     * Kiểm tra quyền chỉnh sửa chapter
     */
    boolean canEditChapter(Long chapterId, Long userId);

    /**
     * Kiểm tra slug đã tồn tại chưa
     */
    boolean isSlugExists(String slug);

    /**
     * Kiểm tra slug đã tồn tại chưa (trừ chapter hiện tại)
     */
    boolean isSlugExists(String slug, Long excludeChapterId);

    /**
     * Kiểm tra chapter number đã tồn tại trong truyện chưa
     */
    boolean isChapterNumberExists(Long storyId, Integer chapterNumber);

    /**
     * Kiểm tra chapter number đã tồn tại trong truyện chưa (trừ chapter hiện tại)
     */
    boolean isChapterNumberExists(Long storyId, Integer chapterNumber, Long excludeChapterId);
}
