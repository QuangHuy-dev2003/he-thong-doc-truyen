package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.Chapter;
import com.meobeo.truyen.domain.response.chapter.ChapterResponse;
import com.meobeo.truyen.domain.response.chapter.ChapterSummaryDto;
import com.meobeo.truyen.repository.ChapterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChapterMapper {

    private final ChapterRepository chapterRepository;

    @Transactional(readOnly = true)
    public ChapterResponse toChapterResponse(Chapter chapter) {
        if (chapter == null) {
            return null;
        }

        ChapterResponse response = new ChapterResponse();
        response.setId(chapter.getId());
        response.setChapterNumber(chapter.getChapterNumber());
        response.setSlug(chapter.getSlug());
        response.setTitle(chapter.getTitle());
        response.setContent(chapter.getContent());
        response.setCreatedAt(chapter.getCreatedAt());

        // Thông tin truyện
        if (chapter.getStory() != null) {
            response.setStoryId(chapter.getStory().getId());
            response.setStoryTitle(chapter.getStory().getTitle());
            response.setStorySlug(chapter.getStory().getSlug());
        }

        // Navigation - lấy chapter trước và sau (chỉ khi cần thiết)
        // setNavigationInfo(response, chapter);

        // Thông tin trạng thái - TODO: implement logic kiểm tra locked/purchased
        response.setIsLocked(false);
        response.setIsPurchased(true);

        return response;
    }

    /**
     * Convert Chapter entity thành ChapterSummaryDto (không có content và
     * navigation)
     */
    public ChapterSummaryDto toChapterSummaryDto(Chapter chapter) {
        if (chapter == null) {
            return null;
        }

        ChapterSummaryDto summary = new ChapterSummaryDto();
        summary.setId(chapter.getId());
        summary.setChapterNumber(chapter.getChapterNumber());
        summary.setSlug(chapter.getSlug());
        summary.setTitle(chapter.getTitle());
        summary.setCreatedAt(chapter.getCreatedAt());

        // Thông tin trạng thái - TODO: implement logic kiểm tra locked/purchased
        summary.setIsLocked(false);
        summary.setIsPurchased(true);

        return summary;
    }

    /**
     * Convert Chapter entity thành ChapterResponse với navigation info
     */
    @Transactional(readOnly = true)
    public ChapterResponse toChapterResponseWithNavigation(Chapter chapter) {
        ChapterResponse response = toChapterResponse(chapter);
        if (response != null && chapter != null) {
            setNavigationInfo(response, chapter);
        }
        return response;
    }

    private void setNavigationInfo(ChapterResponse response, Chapter chapter) {
        try {
            // Chapter trước đó
            chapterRepository.findPreviousChapter(chapter.getStory().getId(), chapter.getChapterNumber())
                    .ifPresent(prevChapter -> {
                        ChapterResponse.ChapterNavigationResponse prevNav = new ChapterResponse.ChapterNavigationResponse();
                        prevNav.setId(prevChapter.getId());
                        prevNav.setChapterNumber(prevChapter.getChapterNumber());
                        prevNav.setSlug(prevChapter.getSlug());
                        prevNav.setTitle(prevChapter.getTitle());
                        response.setPreviousChapter(prevNav);
                    });

            // Chapter tiếp theo
            chapterRepository.findNextChapter(chapter.getStory().getId(), chapter.getChapterNumber())
                    .ifPresent(nextChapter -> {
                        ChapterResponse.ChapterNavigationResponse nextNav = new ChapterResponse.ChapterNavigationResponse();
                        nextNav.setId(nextChapter.getId());
                        nextNav.setChapterNumber(nextChapter.getChapterNumber());
                        nextNav.setSlug(nextChapter.getSlug());
                        nextNav.setTitle(nextChapter.getTitle());
                        response.setNextChapter(nextNav);
                    });
        } catch (Exception e) {
            log.warn("Lỗi khi lấy navigation info cho chapter {}: {}", chapter.getId(), e.getMessage());
        }
    }
}
