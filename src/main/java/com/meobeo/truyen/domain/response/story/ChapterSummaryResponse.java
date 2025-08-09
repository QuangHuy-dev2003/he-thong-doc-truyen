package com.meobeo.truyen.domain.response.story;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterSummaryResponse {

    private Long id;
    private String title;
    private Integer chapterNumber;
    private LocalDateTime createdAt;
    private Boolean isLocked;
}