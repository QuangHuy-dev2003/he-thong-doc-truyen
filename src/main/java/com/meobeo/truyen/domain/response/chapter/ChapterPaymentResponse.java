package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

@Data
public class ChapterPaymentResponse {

    private Long chapterId;
    private Integer price;
    private Boolean isVipOnly;
    private Boolean isLocked;
    private Long storyId;

    // Thông tin chapter
    private Integer chapterNumber;
    private String chapterTitle;
    private String chapterSlug;

    // Thông tin story
    private String storyTitle;
    private String storySlug;
}
