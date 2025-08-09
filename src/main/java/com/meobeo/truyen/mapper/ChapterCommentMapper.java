package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.ChapterComment;
import com.meobeo.truyen.domain.response.comment.CommentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChapterCommentMapper {

    /**
     * Convert ChapterComment entity thành CommentResponse
     */
    public CommentResponse toCommentResponse(ChapterComment comment) {
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

        // Map chapter info
        if (comment.getChapter() != null) {
            CommentResponse.ChapterCommentInfo chapterInfo = new CommentResponse.ChapterCommentInfo();
            chapterInfo.setId(comment.getChapter().getId());
            chapterInfo.setChapterNumber(comment.getChapter().getChapterNumber());
            chapterInfo.setTitle(comment.getChapter().getTitle());

            // Map story info nếu có
            if (comment.getStory() != null) {
                chapterInfo.setStoryId(comment.getStory().getId());
                chapterInfo.setStoryTitle(comment.getStory().getTitle());
            }

            response.setChapter(chapterInfo);
        }

        return response;
    }
}
