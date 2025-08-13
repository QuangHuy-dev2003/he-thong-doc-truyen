package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.entity.StoryViewSession;
import com.meobeo.truyen.domain.response.story.AnalyticsSeriesResponse;
import com.meobeo.truyen.domain.response.story.TopStoriesResponse;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.StoryViewsDailyRepository;
import com.meobeo.truyen.repository.StoryViewSessionRepository;
import com.meobeo.truyen.service.interfaces.StoryViewsService;
import com.meobeo.truyen.utils.ViewSpamProtection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StoryViewsServiceImpl implements StoryViewsService {

    private final StoryViewsDailyRepository storyViewsDailyRepository;
    private final StoryViewSessionRepository storyViewSessionRepository;
    private final StoryRepository storyRepository;
    private final ViewSpamProtection viewSpamProtection;

    @Value("${views.spam.protection.minutes:30}")
    private int spamProtectionMinutes;

    @Override
    public void increaseView(Long storyId) {
        try {
            // Lấy thông tin session và IP
            String sessionId = viewSpamProtection.getSessionId();
            String ipAddress = viewSpamProtection.getClientIpAddress();
            String userAgent = viewSpamProtection.getUserAgent();

            // Kiểm tra spam protection
            LocalDateTime since = LocalDateTime.now().minusMinutes(spamProtectionMinutes);

            boolean sessionExists = storyViewSessionRepository.existsByStoryAndSessionSince(storyId, sessionId, since);
            boolean ipExists = storyViewSessionRepository.existsByStoryAndIpSince(storyId, ipAddress, since);

            if (sessionExists || ipExists) {
                log.debug("Bỏ qua tăng view cho story {} - đã view trong {} phút qua", storyId, spamProtectionMinutes);
                return;
            }

            // Lưu session view
            StoryViewSession session = new StoryViewSession();
            session.setStoryId(storyId);
            session.setSessionId(sessionId);
            session.setIpAddress(ipAddress);
            session.setUserAgent(userAgent);
            storyViewSessionRepository.save(session);

            // Tăng view count
            LocalDate today = LocalDate.now();
            storyViewsDailyRepository.upsertIncrement(storyId, today);

            log.debug("Đã tăng view cho story {} từ IP {} session {}", storyId, ipAddress, sessionId);
        } catch (Exception e) {
            log.error("Lỗi khi tăng view cho story {}: {}", storyId, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "analytics-daily-7d", key = "#storyId + '_' + #start + '_' + #end", unless = "#result.points.isEmpty()")
    public AnalyticsSeriesResponse getDailyViews(Long storyId, LocalDate start, LocalDate end) {
        log.debug("Lấy daily views cho story {} từ {} đến {}", storyId, start, end);
        var rows = storyViewsDailyRepository.findDailyViews(storyId, start, end);
        return AnalyticsSeriesResponse.from(rows);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "analytics-weekly-4w", key = "#storyId + '_' + #start + '_' + #end", unless = "#result.points.isEmpty()")
    public AnalyticsSeriesResponse getWeeklyViews(Long storyId, LocalDate start, LocalDate end) {
        log.debug("Lấy weekly views cho story {} từ {} đến {}", storyId, start, end);
        var rows = storyViewsDailyRepository.findWeeklyViews(storyId, start, end);
        return AnalyticsSeriesResponse.from(rows);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "analytics-monthly-12m", key = "#storyId + '_' + #start + '_' + #end", unless = "#result.points.isEmpty()")
    public AnalyticsSeriesResponse getMonthlyViews(Long storyId, LocalDate start, LocalDate end) {
        log.debug("Lấy monthly views cho story {} từ {} đến {}", storyId, start, end);
        var rows = storyViewsDailyRepository.findMonthlyViews(storyId, start, end);
        return AnalyticsSeriesResponse.from(rows);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "top-stories-7d", key = "'top_' + #start + '_' + #end + '_' + #page + '_' + #size", unless = "#result.content.isEmpty()")
    public TopStoriesResponse getTopStories(LocalDate start, LocalDate end, int page, int size) {
        log.debug("Lấy top stories từ {} đến {}, page {} size {}", start, end, page, size);
        int offset = page * size;
        var rows = storyViewsDailyRepository.findTopStories(start, end, size, offset);
        long totalStories = storyViewsDailyRepository.countStoriesInRange(start, end);

        List<Long> storyIds = rows.stream().map(StoryViewsDailyRepository.TopStoryViewsProjection::getStoryId).toList();
        Map<Long, Story> storyMap = storyRepository.findAllById(storyIds).stream()
                .collect(Collectors.toMap(Story::getId, s -> s));

        return TopStoriesResponse.from(rows, storyMap, page, size, totalStories);
    }

    @Override
    public int cleanupOldData(LocalDate cutoff) {
        return storyViewsDailyRepository.deleteOlderThan(cutoff);
    }

    @Override
    public int cleanupOldSessions(LocalDate cutoff) {
        return storyViewSessionRepository.deleteOlderThan(cutoff.atStartOfDay());
    }
}
