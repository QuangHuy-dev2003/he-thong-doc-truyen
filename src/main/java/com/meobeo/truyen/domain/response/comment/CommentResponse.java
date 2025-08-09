package com.meobeo.truyen.domain.response.comment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {

    private Long id;
    private String content;
    private LocalDateTime createdAt;

    // Thông tin người comment
    private UserCommentInfo user;

    // Thông tin chapter context
    private ChapterCommentInfo chapter;

    @Data
    public static class UserCommentInfo {
        private Long id;
        private String username;
        private String displayName;
        private String avatarUrl;
        private String vipDisplayStyle;
    }

    @Data
    public static class ChapterCommentInfo {
        private Long id;
        private Integer chapterNumber;
        private String title;
        private Long storyId;
        private String storyTitle;
    }
}
