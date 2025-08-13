package com.meobeo.truyen.domain.response.story;

import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.repository.StoryViewsDailyRepository;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class TopStoriesResponse {

    private List<Item> content = new ArrayList<>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static TopStoriesResponse from(List<StoryViewsDailyRepository.TopStoryViewsProjection> rows,
            Map<Long, Story> storyMap,
            int page, int size, long totalElements) {
        TopStoriesResponse resp = new TopStoriesResponse();
        resp.page = page;
        resp.size = size;
        resp.totalElements = totalElements;
        resp.totalPages = (int) Math.ceil((double) totalElements / Math.max(size, 1));
        resp.hasNext = page < resp.totalPages - 1;
        resp.hasPrevious = page > 0;

        for (var r : rows) {
            Story story = storyMap.get(r.getStoryId());
            if (story == null)
                continue;
            Item item = new Item();
            item.setStoryId(story.getId());
            item.setSlug(story.getSlug());
            item.setTitle(story.getTitle());
            item.setCoverImageUrl(story.getCoverImageUrl());
            item.setTotalViews(r.getTotalViews());
            resp.content.add(item);
        }
        return resp;
    }

    @Data
    public static class Item {
        private Long storyId;
        private String slug;
        private String title;
        private String coverImageUrl;
        private Long totalViews;
    }
}
