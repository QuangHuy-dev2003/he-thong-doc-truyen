package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.ReadingHistory;
import com.meobeo.truyen.domain.response.reading.ReadingHistoryResponse;
import com.meobeo.truyen.domain.response.reading.LastReadChapterResponse;
import com.meobeo.truyen.repository.ChapterRepository;
import com.meobeo.truyen.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReadingHistoryMapper {

    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;

    @Transactional(readOnly = true)
    public ReadingHistoryResponse toReadingHistoryResponse(ReadingHistory readingHistory) {
        if (readingHistory == null) {
            return null;
        }

        ReadingHistoryResponse response = new ReadingHistoryResponse();

        // Thông tin chapter
        response.setChapterId(readingHistory.getChapterId());
        response.setChapterNumber(readingHistory.getChapter().getChapterNumber());
        response.setChapterTitle(readingHistory.getChapter().getTitle());
        response.setChapterSlug(readingHistory.getChapter().getSlug());

        // Thông tin story
        response.setStoryId(readingHistory.getStory().getId());
        response.setStoryTitle(readingHistory.getStory().getTitle());
        response.setStorySlug(readingHistory.getStory().getSlug());
        response.setStoryCoverImageUrl(readingHistory.getStory().getCoverImageUrl());
        response.setAuthorName(readingHistory.getStory().getAuthorName());

        // Thông tin đọc
        response.setLastReadAt(readingHistory.getLastReadAt());

        // Tính toán thống kê đọc
        try {
            Long storyId = readingHistory.getStory().getId();
            response.setTotalChaptersInStory(storyRepository.countChaptersByStoryId(storyId));

            // Với logic mới, mỗi story chỉ có 1 bản ghi, nên số chapter đã đọc = 1
            response.setReadChaptersInStory(1L);

            // Tính phần trăm đã đọc (dựa trên chapter number)
            if (response.getTotalChaptersInStory() > 0) {
                double progress = (double) readingHistory.getChapter().getChapterNumber()
                        / response.getTotalChaptersInStory() * 100;
                response.setReadingProgress(Math.min(progress, 100.0));
            } else {
                response.setReadingProgress(0.0);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tính thống kê đọc cho story {}: {}",
                    readingHistory.getStory().getId(), e.getMessage());
            response.setTotalChaptersInStory(0L);
            response.setReadChaptersInStory(1L);
            response.setReadingProgress(0.0);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public LastReadChapterResponse toLastReadChapterResponse(ReadingHistory readingHistory) {
        if (readingHistory == null) {
            return null;
        }

        LastReadChapterResponse response = new LastReadChapterResponse();

        // Thông tin chapter
        response.setChapterId(readingHistory.getChapterId());
        response.setChapterNumber(readingHistory.getChapter().getChapterNumber());
        response.setChapterTitle(readingHistory.getChapter().getTitle());
        response.setChapterSlug(readingHistory.getChapter().getSlug());

        // Thông tin story
        response.setStoryId(readingHistory.getStory().getId());
        response.setStoryTitle(readingHistory.getStory().getTitle());
        response.setStorySlug(readingHistory.getStory().getSlug());
        response.setStoryCoverImageUrl(readingHistory.getStory().getCoverImageUrl());
        response.setAuthorName(readingHistory.getStory().getAuthorName());

        // Thông tin đọc
        response.setLastReadAt(readingHistory.getLastReadAt());

        // Tìm chapter tiếp theo và trước đó
        try {
            Long storyId = readingHistory.getStory().getId();
            Integer currentChapterNumber = readingHistory.getChapter().getChapterNumber();

            // Tìm chapter tiếp theo
            var nextChapter = chapterRepository.findNextChapter(storyId, currentChapterNumber);
            if (nextChapter.isPresent()) {
                response.setHasNextChapter(true);
                response.setNextChapterId(nextChapter.get().getId());
                response.setNextChapterNumber(nextChapter.get().getChapterNumber());
            } else {
                response.setHasNextChapter(false);
            }

            // Tìm chapter trước đó
            var prevChapter = chapterRepository.findPreviousChapter(storyId, currentChapterNumber);
            if (prevChapter.isPresent()) {
                response.setHasPreviousChapter(true);
                response.setPreviousChapterId(prevChapter.get().getId());
                response.setPreviousChapterNumber(prevChapter.get().getChapterNumber());
            } else {
                response.setHasPreviousChapter(false);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tìm chapter tiếp theo/trước đó cho story {}: {}",
                    readingHistory.getStory().getId(), e.getMessage());
            response.setHasNextChapter(false);
            response.setHasPreviousChapter(false);
        }

        return response;
    }
}
