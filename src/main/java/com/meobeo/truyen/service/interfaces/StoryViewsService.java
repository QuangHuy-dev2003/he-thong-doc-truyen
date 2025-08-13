package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.response.story.AnalyticsSeriesResponse;
import com.meobeo.truyen.domain.response.story.TopStoriesResponse;

import java.time.LocalDate;

public interface StoryViewsService {

    /**
     * Tăng view cho story với bảo vệ chống spam
     */
    void increaseView(Long storyId);

    /**
     * Tăng view bất đồng bộ để tránh conflict với read-only transaction
     */
    void increaseViewAsync(Long storyId);

    /**
     * Lấy analytics theo ngày với cache
     */
    AnalyticsSeriesResponse getDailyViews(Long storyId, LocalDate start, LocalDate end);

    /**
     * Lấy analytics theo tuần với cache
     */
    AnalyticsSeriesResponse getWeeklyViews(Long storyId, LocalDate start, LocalDate end);

    /**
     * Lấy analytics theo tháng với cache
     */
    AnalyticsSeriesResponse getMonthlyViews(Long storyId, LocalDate start, LocalDate end);

    /**
     * Lấy top stories với cache
     */
    TopStoriesResponse getTopStories(LocalDate start, LocalDate end, int page, int size);

    /**
     * Dọn dữ liệu cũ
     */
    int cleanupOldData(LocalDate cutoff);

    /**
     * Dọn session cũ
     */
    int cleanupOldSessions(LocalDate cutoff);
}
