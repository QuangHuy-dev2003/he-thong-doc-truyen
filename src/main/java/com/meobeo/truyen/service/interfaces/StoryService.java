package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.request.story.CreateStoryRequest;
import com.meobeo.truyen.domain.request.story.StorySearchRequest;
import com.meobeo.truyen.domain.request.story.UpdateStoryRequest;
import com.meobeo.truyen.domain.response.story.StoryListResponse;
import com.meobeo.truyen.domain.response.story.StoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StoryService {

    /**
     * Tạo truyện mới
     */
    StoryResponse createStory(CreateStoryRequest request, Long authorId);

    /**
     * Cập nhật truyện
     */
    StoryResponse updateStory(Long storyId, UpdateStoryRequest request, Long userId);

    /**
     * Xóa truyện
     */
    void deleteStory(Long storyId, Long userId);

    /**
     * Lấy chi tiết truyện theo ID hoặc slug
     */
    StoryResponse getStoryDetail(String identifier, Long userId);

    /**
     * Tìm kiếm và lọc truyện
     */
    StoryListResponse searchStories(StorySearchRequest request);

    /**
     * Lấy danh sách truyện của một tác giả
     */
    StoryListResponse getStoriesByAuthor(Long authorId, Pageable pageable);

    /**
     * Kiểm tra quyền chỉnh sửa truyện
     */
    boolean canEditStory(Long storyId, Long userId);

    /**
     * Kiểm tra slug đã tồn tại chưa
     */
    boolean isSlugExists(String slug);

    /**
     * Kiểm tra slug đã tồn tại chưa (trừ story hiện tại)
     */
    boolean isSlugExists(String slug, Long excludeStoryId);
}