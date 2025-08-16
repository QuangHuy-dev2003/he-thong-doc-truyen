package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.response.chapter.ChapterLockStatusResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockedChaptersResponse;
import com.meobeo.truyen.service.interfaces.ChapterUnlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChapterUnlockMapper {

    private final ChapterUnlockService chapterUnlockService;

    /**
     * Tạo ChapterLockStatusResponse từ chapterId và userId
     */
    public ChapterLockStatusResponse toChapterLockStatusResponse(Long chapterId, Long userId) {
        ChapterLockStatusResponse response = new ChapterLockStatusResponse();
        response.setChapterId(chapterId);
        response.setIsLocked(chapterUnlockService.isChapterLocked(chapterId));
        response.setIsUnlockedByUser(chapterUnlockService.hasUserUnlockedChapter(userId, chapterId));
        return response;
    }

    /**
     * Tạo UnlockedChaptersResponse từ danh sách chapter IDs (không phân trang)
     */
    public UnlockedChaptersResponse toUnlockedChaptersResponse(List<Long> chapterIds, Long storyId, String storyTitle) {
        return UnlockedChaptersResponse.fromChapterIds(chapterIds, storyId, storyTitle);
    }

    /**
     * Tạo UnlockedChaptersResponse từ Page (có phân trang)
     */
    public UnlockedChaptersResponse toUnlockedChaptersResponse(Page<Long> page, Long storyId, String storyTitle) {
        return UnlockedChaptersResponse.fromPage(page, storyId, storyTitle);
    }
}
