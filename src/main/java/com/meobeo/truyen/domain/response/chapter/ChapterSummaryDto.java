package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterSummaryDto {

    private Long id;
    private Integer chapterNumber;
    private String slug;
    private String title;
    private LocalDateTime createdAt;

    // Thông tin trạng thái cơ bản
    private Boolean isLocked;
    private Boolean isUnlockedByUser;
    private Integer unlockPrice;
}
