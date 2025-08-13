package com.meobeo.truyen.domain.response.reading;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LastReadChapterResponse {

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
    private Boolean hasNextChapter;
    private Boolean hasPreviousChapter;
    private Long nextChapterId;
    private Long previousChapterId;
    private Integer nextChapterNumber;
    private Integer previousChapterNumber;
}
