package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.response.reading.ReadingHistoryListResponse;
import com.meobeo.truyen.domain.response.reading.ReadingHistoryResponse;
import com.meobeo.truyen.domain.response.reading.LastReadChapterResponse;
import org.springframework.data.domain.Pageable;

public interface ReadingHistoryService {

    /**
     * Ghi lại lịch sử đọc chapter (cập nhật chapter mới nhất cho story)
     */
    ReadingHistoryResponse recordReading(Long chapterId, Long userId);

    /**
     * Lấy danh sách lịch sử đọc của user (mỗi story chỉ 1 bản ghi)
     */
    ReadingHistoryListResponse getUserReadingHistory(Long userId, Pageable pageable);

    /**
     * Lấy lịch sử đọc của user cho một story cụ thể
     */
    ReadingHistoryResponse getUserReadingHistoryByStory(Long userId, Long storyId);

    /**
     * Lấy chapter cuối cùng user đã đọc trong story
     */
    LastReadChapterResponse getLastReadChapter(Long userId, Long storyId);

    /**
     * Đếm số story đã đọc của user
     */
    Long countUserReadStories(Long userId);

    /**
     * Xóa lịch sử đọc của user cho một story
     */
    void deleteReadingHistory(Long userId, Long storyId);

    /**
     * Xóa tất cả lịch sử đọc của user
     */
    void clearUserReadingHistory(Long userId);

    /**
     * Kiểm tra user đã đọc story chưa
     */
    boolean hasUserReadStory(Long userId, Long storyId);

    /**
     * Lấy danh sách story đã đọc của user, sắp xếp theo thời gian đọc gần nhất
     */
    ReadingHistoryListResponse getLastReadStoriesByUser(Long userId);
}
