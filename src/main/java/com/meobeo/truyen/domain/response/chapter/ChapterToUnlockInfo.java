package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

@Data
public class ChapterToUnlockInfo {
    private Long chapterId;
    private Integer chapterNumber;
    private String chapterTitle;
    private Integer price;

    public ChapterToUnlockInfo(Long chapterId, Integer chapterNumber, String chapterTitle, Integer price) {
        this.chapterId = chapterId;
        this.chapterNumber = chapterNumber;
        this.chapterTitle = chapterTitle;
        this.price = price;
    }
}
