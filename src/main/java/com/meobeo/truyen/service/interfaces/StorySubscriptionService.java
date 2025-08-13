package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.response.subscription.SubscriptionListResponse;
import com.meobeo.truyen.domain.response.subscription.SubscriptionResponse;
import org.springframework.data.domain.Pageable;

public interface StorySubscriptionService {

    /**
     * Đăng ký theo dõi truyện
     */
    SubscriptionResponse subscribeToStory(Long storyId, Long userId);

    /**
     * Hủy đăng ký theo dõi truyện
     */
    void unsubscribeFromStory(Long storyId, Long userId);

    /**
     * Lấy danh sách truyện đang theo dõi của user
     */
    SubscriptionListResponse getUserSubscriptions(Long userId, Pageable pageable);

    /**
     * Kiểm tra user đã đăng ký theo dõi truyện chưa
     */
    boolean isUserSubscribedToStory(Long storyId, Long userId);

    /**
     * Đếm số truyện đang theo dõi của user
     */
    Long countUserSubscriptions(Long userId);

    /**
     * Đếm số người đăng ký theo dõi truyện
     */
    Long countStorySubscribers(Long storyId);

    /**
     * Toggle trạng thái đăng ký theo dõi (nếu đã đăng ký thì hủy, chưa thì đăng ký)
     */
    SubscriptionResponse toggleSubscription(Long storyId, Long userId);
}
