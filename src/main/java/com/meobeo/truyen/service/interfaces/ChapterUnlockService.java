package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.chapter.UnlockChapterRequest;
import com.meobeo.truyen.domain.request.chapter.UnlockChapterRangeRequest;
import com.meobeo.truyen.domain.request.chapter.UnlockFullStoryRequest;
import com.meobeo.truyen.domain.response.chapter.UnlockChapterBatchResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockChapterResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockFullStoryResponse;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChapterUnlockService {

    /**
     * Mở khóa 1 chương
     */
    UnlockChapterResponse unlockChapter(UnlockChapterRequest request, Long userId);

    /**
     * Mở khóa từ chương A đến chương B
     */
    UnlockChapterBatchResponse unlockChapterRange(UnlockChapterRangeRequest request, Long userId);

    /**
     * Mở khóa full truyện
     */
    UnlockFullStoryResponse unlockFullStory(UnlockFullStoryRequest request, Long userId);

    /**
     * Kiểm tra chapter có bị khóa không
     */
    boolean isChapterLocked(Long chapterId);

    /**
     * Kiểm tra user đã mở khóa chapter chưa
     */
    boolean hasUserUnlockedChapter(Long userId, Long chapterId);

    /**
     * Lấy danh sách chapter đã mở khóa của user trong story (không phân trang)
     */
    List<Long> getUserUnlockedChapterIds(Long userId, Long storyId);

    /**
     * Lấy danh sách chapter đã mở khóa của user trong story (có phân trang)
     */
    Page<Long> getUserUnlockedChapterIds(Long userId, Long storyId, Pageable pageable);

    /**
     * Tính giá mở khóa (có tính discount)
     */
    int calculateUnlockPrice(int originalPrice, int chapterCount, boolean isFullUnlock);
}
