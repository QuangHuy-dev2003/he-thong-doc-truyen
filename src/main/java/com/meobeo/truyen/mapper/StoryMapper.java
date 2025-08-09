package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.entity.Chapter;
import com.meobeo.truyen.domain.entity.Genre;
import com.meobeo.truyen.domain.response.story.StoryResponse;
import com.meobeo.truyen.domain.response.story.GenreResponse;
import com.meobeo.truyen.domain.response.story.ChapterSummaryResponse;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.ChapterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoryMapper {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;

    @Transactional(readOnly = true)
    public StoryResponse toStoryResponse(Story story) {
        if (story == null) {
            return null;
        }

        StoryResponse response = new StoryResponse();
        response.setId(story.getId());
        response.setTitle(story.getTitle());
        response.setSlug(story.getSlug());
        response.setDescription(story.getDescription());
        response.setCoverImageUrl(story.getCoverImageUrl());
        response.setStatus(story.getStatus());
        response.setCreatedAt(story.getCreatedAt());

        // Thông tin tác giả
        if (story.getAuthor() != null) {
            response.setAuthorId(story.getAuthor().getId());
            response.setAuthorName(story.getAuthorName());
            response.setAuthorUsername(story.getAuthor().getUsername());
        }

        // Thông tin thể loại - xử lý an toàn
        Set<GenreResponse> genreResponses = new HashSet<>();
        if (story.getGenres() != null) {
            try {
                // Tạo copy an toàn để tránh ConcurrentModificationException
                Set<Genre> genresCopy = new HashSet<>(story.getGenres());
                for (Genre genre : genresCopy) {
                    genreResponses.add(toGenreResponse(genre));
                }
            } catch (Exception e) {
                log.warn("Lỗi khi xử lý genres cho story {}: {}", story.getId(), e.getMessage());
            }
        }
        response.setGenres(genreResponses);

        // Thống kê
        response.setChapterCount(storyRepository.countChaptersByStoryId(story.getId()));
        response.setViewCount(storyRepository.countViewsByStoryId(story.getId()));
        response.setFavoriteCount(storyRepository.countFavoritesByStoryId(story.getId()));
        response.setVoteCount(storyRepository.countVotesByStoryId(story.getId()));
        response.setAverageRating(storyRepository.getAverageRatingByStoryId(story.getId()));

        // Chapter mới nhất
        Set<ChapterSummaryResponse> latestChapters = getLatestChapters(story.getId());
        response.setLatestChapters(latestChapters);

        return response;
    }

    private GenreResponse toGenreResponse(Genre genre) {
        if (genre == null) {
            return null;
        }

        GenreResponse response = new GenreResponse();
        response.setId(genre.getId());
        response.setName(genre.getName());
        return response;
    }

    private Set<ChapterSummaryResponse> getLatestChapters(Long storyId) {
        try {
            // Lấy 5 chapter mới nhất
            List<Chapter> chapters = chapterRepository.findTop5ByStoryIdOrderByChapterNumberDesc(storyId);

            Set<ChapterSummaryResponse> chapterResponses = new HashSet<>();
            for (Chapter chapter : chapters) {
                chapterResponses.add(toChapterSummaryResponse(chapter));
            }
            return chapterResponses;
        } catch (Exception e) {
            log.warn("Lỗi khi lấy latest chapters cho story {}: {}", storyId, e.getMessage());
            return new HashSet<>();
        }
    }

    private ChapterSummaryResponse toChapterSummaryResponse(Chapter chapter) {
        if (chapter == null) {
            return null;
        }

        ChapterSummaryResponse response = new ChapterSummaryResponse();
        response.setId(chapter.getId());
        response.setTitle(chapter.getTitle());
        response.setChapterNumber(chapter.getChapterNumber());
        response.setCreatedAt(chapter.getCreatedAt());
        // TODO: Implement logic kiểm tra chapter có bị khóa không
        response.setIsLocked(false);
        return response;
    }
}
