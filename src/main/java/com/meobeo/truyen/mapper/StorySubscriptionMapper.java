package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.StorySubscription;
import com.meobeo.truyen.domain.response.subscription.SubscriptionResponse;
import com.meobeo.truyen.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorySubscriptionMapper {

    private final StoryRepository storyRepository;

    @Transactional(readOnly = true)
    public SubscriptionResponse toSubscriptionResponse(StorySubscription subscription) {
        if (subscription == null) {
            return null;
        }

        SubscriptionResponse response = new SubscriptionResponse();

        // Thông tin cơ bản
        response.setStoryId(subscription.getStory().getId());
        response.setStoryTitle(subscription.getStory().getTitle());
        response.setStorySlug(subscription.getStory().getSlug());
        response.setStoryCoverImageUrl(subscription.getStory().getCoverImageUrl());
        response.setSubscribedAt(subscription.getSubscribedAt());
        response.setIsActive(subscription.getIsActive());

        // Thông tin tác giả - chỉ lấy từ Story entity
        response.setAuthorName(subscription.getStory().getAuthorName());
        response.setAuthorUsername(null); // Không có authorUsername trong Story entity

        // Thống kê truyện
        try {
            response.setChapterCount(storyRepository.countChaptersByStoryId(subscription.getStory().getId()));
            response.setViewCount(storyRepository.countViewsByStoryId(subscription.getStory().getId()));
            response.setFavoriteCount(storyRepository.countFavoritesByStoryId(subscription.getStory().getId()));
            response.setAverageRating(storyRepository.getAverageRatingByStoryId(subscription.getStory().getId()));
        } catch (Exception e) {
            log.warn("Lỗi khi lấy thống kê cho story {}: {}", subscription.getStory().getId(), e.getMessage());
            // Set giá trị mặc định nếu có lỗi
            response.setChapterCount(0L);
            response.setViewCount(0L);
            response.setFavoriteCount(0L);
            response.setAverageRating(0.0);
        }

        return response;
    }
}
