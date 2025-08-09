package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterResponse {

    private Long id;
    private Integer chapterNumber;
    private String slug;
    private String title;
    private String content;
    private LocalDateTime createdAt;

    // Thông tin truyện
    private Long storyId;
    private String storyTitle;
    private String storySlug;

    // Thông tin navigation
    private ChapterNavigationResponse previousChapter;
    private ChapterNavigationResponse nextChapter;

    // Thông tin trạng thái và payment
    private Boolean isLocked;
    private Boolean isPurchased;
    private Integer price;
    private Boolean isVipOnly;

    @Data
    public static class ChapterNavigationResponse {
        private Long id;
        private Integer chapterNumber;
        private String slug;
        private String title;
    }
}
