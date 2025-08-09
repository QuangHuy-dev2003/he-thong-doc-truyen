package com.meobeo.truyen.domain.response.story;

import com.meobeo.truyen.domain.enums.StoryStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class StoryResponse {

    private Long id;
    private String title;
    private String slug;
    private String description;
    private String coverImageUrl;
    private StoryStatus status;
    private LocalDateTime createdAt;

    // Thông tin tác giả
    private Long authorId;
    private String authorName;
    private String authorUsername;

    // Thông tin thể loại
    private Set<GenreResponse> genres;

    // Thống kê
    private Long chapterCount;
    private Long viewCount;
    private Long favoriteCount;
    private Long voteCount;
    private Double averageRating;

    // Danh sách chapter mới nhất
    private Set<ChapterSummaryResponse> latestChapters;
}