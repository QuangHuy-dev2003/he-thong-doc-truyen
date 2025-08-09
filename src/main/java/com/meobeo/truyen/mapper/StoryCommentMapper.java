package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.StoryComment;
import com.meobeo.truyen.domain.response.comment.CommentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoryCommentMapper {

    /**
     * Convert StoryComment entity thành CommentResponse
     */
    public CommentResponse toCommentResponse(StoryComment comment) {
        if (comment == null) {
            return null;
        }

        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCreatedAt());

        // Map user info
        if (comment.getUser() != null) {
            CommentResponse.UserCommentInfo userInfo = new CommentResponse.UserCommentInfo();
            userInfo.setId(comment.getUser().getId());
            userInfo.setUsername(comment.getUser().getUsername());
            userInfo.setDisplayName(comment.getUser().getDisplayName());
            userInfo.setAvatarUrl(comment.getUser().getAvatarUrl());
            userInfo.setVipDisplayStyle(comment.getUser().getVipDisplayStyle());
            response.setUser(userInfo);
        }

        // Map story info cho StoryComment (khác với ChapterComment)
        if (comment.getStory() != null) {
            CommentResponse.ChapterCommentInfo storyInfo = new CommentResponse.ChapterCommentInfo();
            // Sử dụng lại ChapterCommentInfo nhưng chỉ set story fields
            storyInfo.setStoryId(comment.getStory().getId());
            storyInfo.setStoryTitle(comment.getStory().getTitle());
            // Không set chapter info vì đây là story comment
            response.setChapter(storyInfo);
        }

        return response;
    }
}
