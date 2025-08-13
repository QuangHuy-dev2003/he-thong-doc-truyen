package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.Favorite;
import com.meobeo.truyen.domain.response.favorite.FavoriteResponse;
import com.meobeo.truyen.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FavoriteMapper {

    private final StoryRepository storyRepository;

    @Transactional(readOnly = true)
    public FavoriteResponse toFavoriteResponse(Favorite favorite) {
        if (favorite == null) {
            return null;
        }

        FavoriteResponse response = new FavoriteResponse();

        // Thông tin cơ bản
        response.setStoryId(favorite.getStory().getId());
        response.setStoryTitle(favorite.getStory().getTitle());
        response.setStorySlug(favorite.getStory().getSlug());
        response.setStoryCoverImageUrl(favorite.getStory().getCoverImageUrl());
        response.setFavoritedAt(favorite.getFavoritedAt());

        // Thông tin tác giả - chỉ lấy từ Story entity
        response.setAuthorName(favorite.getStory().getAuthorName());
        response.setAuthorUsername(null); // Không có authorUsername trong Story entity

        // Thống kê truyện
        try {
            response.setChapterCount(storyRepository.countChaptersByStoryId(favorite.getStory().getId()));
            response.setViewCount(storyRepository.countViewsByStoryId(favorite.getStory().getId()));
            response.setFavoriteCount(storyRepository.countFavoritesByStoryId(favorite.getStory().getId()));
            response.setAverageRating(storyRepository.getAverageRatingByStoryId(favorite.getStory().getId()));
        } catch (Exception e) {
            log.warn("Lỗi khi lấy thống kê cho story {}: {}", favorite.getStory().getId(), e.getMessage());
            // Set giá trị mặc định nếu có lỗi
            response.setChapterCount(0L);
            response.setViewCount(0L);
            response.setFavoriteCount(0L);
            response.setAverageRating(0.0);
        }

        return response;
    }
}
