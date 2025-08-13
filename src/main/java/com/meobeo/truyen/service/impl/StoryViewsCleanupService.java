package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.service.interfaces.StoryViewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryViewsCleanupService {

    private final StoryViewsService storyViewsService;

    @Value("${views.retention.days:365}")
    private int retentionDays;

    @Value("${views.session.retention.days:7}")
    private int sessionRetentionDays;

    /**
     * Dọn dẹp dữ liệu views cũ hơn retentionDays mỗi ngày lúc 03:00 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldViews() {
        LocalDate cutoff = LocalDate.now().minusDays(retentionDays);
        try {
            int deleted = storyViewsService.cleanupOldData(cutoff);
            log.info("Dọn dẹp story_views_daily trước {}: đã xóa {} bản ghi", cutoff, deleted);
        } catch (Exception e) {
            log.error("Lỗi dọn dẹp story_views_daily: {}", e.getMessage(), e);
        }
    }

    /**
     * Dọn dẹp session cũ hơn sessionRetentionDays mỗi ngày lúc 03:30 AM
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanupOldSessions() {
        LocalDate cutoff = LocalDate.now().minusDays(sessionRetentionDays);
        try {
            int deleted = storyViewsService.cleanupOldSessions(cutoff);
            log.info("Dọn dẹp story_view_sessions trước {}: đã xóa {} bản ghi", cutoff, deleted);
        } catch (Exception e) {
            log.error("Lỗi dọn dẹp story_view_sessions: {}", e.getMessage(), e);
        }
    }
}
