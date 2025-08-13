package com.meobeo.truyen.domain.response.reading;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadingHistoryResponse {

    private Long chapterId;
    private Integer chapterNumber;
    private String chapterTitle;
    private String chapterSlug;
    private Long storyId;
    private String storyTitle;
    private String storySlug;
    private String storyCoverImageUrl;
    private String authorName;
    private LocalDateTime lastReadAt;
    private Long totalChaptersInStory;
    private Long readChaptersInStory;
    private Double readingProgress; // Phần trăm đã đọc trong story
}
