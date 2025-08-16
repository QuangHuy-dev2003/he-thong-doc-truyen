package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.Chapter;
import com.meobeo.truyen.domain.entity.ChapterPayment;
import com.meobeo.truyen.domain.response.chapter.ChapterResponse;
import com.meobeo.truyen.domain.response.chapter.ChapterSummaryDto;
import com.meobeo.truyen.repository.ChapterPaymentRepository;
import com.meobeo.truyen.repository.ChapterRepository;
import com.meobeo.truyen.domain.repository.ChapterUnlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChapterMapper {

    private final ChapterRepository chapterRepository;
    private final ChapterPaymentRepository chapterPaymentRepository;
    private final ChapterUnlockRepository chapterUnlockRepository;

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

        // Thông tin trạng thái và payment
        setChapterStatusInfo(response, chapter, null);

        return response;
    }

    /**
     * Convert Chapter entity thành ChapterResponse với thông tin user cụ thể
     */
    @Transactional(readOnly = true)
    public ChapterResponse toChapterResponse(Chapter chapter, Long userId) {
        ChapterResponse response = toChapterResponse(chapter);
        if (response != null && chapter != null) {
            setChapterStatusInfo(response, chapter, userId);
        }
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

        // Thông tin trạng thái và payment
        setChapterStatusInfo(summary, chapter, null);

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

    /**
     * Set thông tin trạng thái khóa/mở khóa cho ChapterResponse
     */
    private void setChapterStatusInfo(ChapterResponse response, Chapter chapter, Long userId) {
        // Lấy thông tin payment
        ChapterPayment payment = chapterPaymentRepository.findById(chapter.getId()).orElse(null);

        if (payment != null) {
            response.setIsLocked(Boolean.TRUE.equals(payment.getIsLocked()));
            response.setIsVipOnly(Boolean.TRUE.equals(payment.getIsVipOnly()));
            response.setUnlockPrice(payment.getPrice());

            // Kiểm tra user đã mở khóa chưa
            if (userId != null) {
                boolean isUnlockedByUser = chapterUnlockRepository.existsByUserIdAndChapterId(userId, chapter.getId());
                response.setIsUnlockedByUser(isUnlockedByUser);
            } else {
                response.setIsUnlockedByUser(false);
            }
        } else {
            response.setIsLocked(false);
            response.setIsVipOnly(false);
            response.setUnlockPrice(0);
            response.setIsUnlockedByUser(false);
        }
    }

    /**
     * Set thông tin trạng thái khóa/mở khóa cho ChapterSummaryDto
     */
    private void setChapterStatusInfo(ChapterSummaryDto summary, Chapter chapter, Long userId) {
        // Lấy thông tin payment
        ChapterPayment payment = chapterPaymentRepository.findById(chapter.getId()).orElse(null);

        if (payment != null) {
            summary.setIsLocked(Boolean.TRUE.equals(payment.getIsLocked()));
            summary.setUnlockPrice(payment.getPrice());

            // Kiểm tra user đã mở khóa chưa
            if (userId != null) {
                boolean isUnlockedByUser = chapterUnlockRepository.existsByUserIdAndChapterId(userId, chapter.getId());
                summary.setIsUnlockedByUser(isUnlockedByUser);
            } else {
                summary.setIsUnlockedByUser(false);
            }
        } else {
            summary.setIsLocked(false);
            summary.setUnlockPrice(0);
            summary.setIsUnlockedByUser(false);
        }
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
