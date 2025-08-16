package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.chapter.UnlockChapterRangeRequest;
import com.meobeo.truyen.domain.request.chapter.UnlockFullStoryRequest;
import com.meobeo.truyen.domain.response.chapter.UnlockChapterBatchResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockChapterResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockFullStoryResponse;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChapterUnlockService {

    /**
     * Mở khóa 1 chương
     */
    UnlockChapterResponse unlockChapter(Long chapterId, Long userId);

    /**
     * Mở khóa từ chương A đến chương B
     */
    UnlockChapterBatchResponse unlockChapterRange(Long storyId, UnlockChapterRangeRequest request, Long userId);

    /**
     * Mở khóa full truyện
     */
    UnlockFullStoryResponse unlockFullStory(Long storyId, UnlockFullStoryRequest request, Long userId);

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

    /**
     * Bắt đầu mở khóa range chương bất đồng bộ
     */
    String startAsyncUnlockRange(Long storyId, UnlockChapterRangeRequest request, Long userId);

    /**
     * Bắt đầu mở khóa full truyện bất đồng bộ
     */
    String startAsyncUnlockFullStory(Long storyId, UnlockFullStoryRequest request, Long userId);

    /**
     * Lấy trạng thái job unlock range
     */
    Optional<UnlockChapterBatchResponse> getAsyncUnlockRangeStatus(String jobId);

    /**
     * Lấy trạng thái job unlock full story
     */
    Optional<UnlockFullStoryResponse> getAsyncUnlockFullStoryStatus(String jobId);

    /**
     * Hủy job unlock range
     */
    boolean cancelAsyncUnlockRange(String jobId, Long userId);

    /**
     * Hủy job unlock full story
     */
    boolean cancelAsyncUnlockFullStory(String jobId, Long userId);

    /**
     * Check trạng thái unlock full truyện trước khi mở khóa
     */
    UnlockFullStoryResponse checkUnlockFullStoryStatus(Long storyId, Long userId);
}
