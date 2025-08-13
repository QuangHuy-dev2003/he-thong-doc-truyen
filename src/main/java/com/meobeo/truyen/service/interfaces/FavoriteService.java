package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.response.favorite.FavoriteListResponse;
import com.meobeo.truyen.domain.response.favorite.FavoriteResponse;
import org.springframework.data.domain.Pageable;

public interface FavoriteService {

    /**
     * Thêm truyện vào danh sách yêu thích
     */
    FavoriteResponse addToFavorite(Long storyId, Long userId);

    /**
     * Xóa truyện khỏi danh sách yêu thích
     */
    void removeFromFavorite(Long storyId, Long userId);

    /**
     * Lấy danh sách truyện yêu thích của user
     */
    FavoriteListResponse getUserFavorites(Long userId, Pageable pageable);

    /**
     * Kiểm tra user đã yêu thích truyện chưa
     */
    boolean isUserFavoriteStory(Long storyId, Long userId);

    /**
     * Đếm số truyện yêu thích của user
     */
    Long countUserFavorites(Long userId);

    /**
     * Đếm số người yêu thích truyện
     */
    Long countStoryFavorites(Long storyId);
}
