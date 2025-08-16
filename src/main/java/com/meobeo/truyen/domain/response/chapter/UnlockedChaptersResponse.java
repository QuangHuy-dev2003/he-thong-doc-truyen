package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class UnlockedChaptersResponse {

    private Long storyId;
    private String storyTitle;
    private List<Long> unlockedChapterIds;
    private Integer totalUnlocked;

    // Thông tin phân trang
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static UnlockedChaptersResponse fromChapterIds(List<Long> chapterIds, Long storyId, String storyTitle) {
        UnlockedChaptersResponse response = new UnlockedChaptersResponse();
        response.setStoryId(storyId);
        response.setStoryTitle(storyTitle);
        response.setUnlockedChapterIds(chapterIds);
        response.setTotalUnlocked(chapterIds.size());
        return response;
    }

    public static UnlockedChaptersResponse fromPage(Page<Long> page, Long storyId, String storyTitle) {
        UnlockedChaptersResponse response = new UnlockedChaptersResponse();
        response.setStoryId(storyId);
        response.setStoryTitle(storyTitle);
        response.setUnlockedChapterIds(page.getContent());
        response.setTotalUnlocked((int) page.getTotalElements());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
        return response;
    }
}
