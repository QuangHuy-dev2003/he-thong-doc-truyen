package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.*;
import com.meobeo.truyen.domain.request.story.CreateStoryRequest;
import com.meobeo.truyen.domain.request.story.StorySearchRequest;
import com.meobeo.truyen.domain.request.story.UpdateStoryRequest;
import com.meobeo.truyen.domain.response.story.*;
import com.meobeo.truyen.exception.ForbiddenException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.repository.*;
import com.meobeo.truyen.service.interfaces.AsyncCloudinaryService;
import com.meobeo.truyen.service.interfaces.CloudinaryService;
import com.meobeo.truyen.service.interfaces.StoryViewsService;
import com.meobeo.truyen.service.interfaces.StoryService;
import com.meobeo.truyen.utils.SecurityUtils;
import com.meobeo.truyen.mapper.StoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final GenreRepository genreRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final AsyncCloudinaryService asyncCloudinaryService;
    private final SecurityUtils securityUtils;
    private final StoryMapper storyMapper;
    private final StoryViewsService storyViewsService;

    @Override
    public StoryResponse createStory(CreateStoryRequest request, Long authorId) {
        log.info("Tạo truyện mới: title={}, authorId={}", request.getTitle(), authorId);

        // Kiểm tra quyền tạo truyện
        User author = getUserById(authorId);
        validateAuthorPermissions(author);

        // Validate slug
        if (isSlugExists(request.getSlug())) {
            throw new IllegalArgumentException("Slug đã tồn tại: " + request.getSlug());
        }

        // Validate genres
        Set<Genre> genres = validateAndGetGenres(request.getGenreIds());

        // Tạo truyện mới (không có ảnh bìa ban đầu)
        Story story = new Story();
        story.setTitle(request.getTitle());
        story.setSlug(request.getSlug());
        story.setDescription(request.getDescription());
        story.setAuthorName(request.getAuthorName());
        story.setStatus(request.getStatus());
        story.setCoverImageUrl(null); // Sẽ cập nhật sau khi upload
        story.setAuthor(author); // Người đăng truyện
        story.setGenres(genres);

        Story savedStory = storyRepository.save(story);
        log.info("Tạo truyện thành công: storyId={}", savedStory.getId());

        // Upload ảnh bìa bất đồng bộ nếu có
        if (request.getCoverImage() != null) {
            asyncCloudinaryService.uploadStoryCoverImageAsync(savedStory.getId(), request.getCoverImage());
        }

        return storyMapper.toStoryResponse(savedStory);
    }

    @Override
    public StoryResponse updateStory(Long storyId, UpdateStoryRequest request, Long userId) {
        log.info("Cập nhật truyện: storyId={}, userId={}", storyId, userId);

        // Lấy truyện và kiểm tra quyền
        Story story = getStoryById(storyId);
        validateEditPermissions(story, userId);

        // Validate slug nếu có thay đổi
        if (!story.getSlug().equals(request.getSlug()) && isSlugExists(request.getSlug(), storyId)) {
            throw new IllegalArgumentException("Slug đã tồn tại: " + request.getSlug());
        }

        // Validate genres
        Set<Genre> genres = validateAndGetGenres(request.getGenreIds());

        // Xử lý ảnh bìa mới nếu có
        String coverImageUrl = story.getCoverImageUrl();
        if (request.getCoverImage() != null) {
            // Xóa ảnh cũ bất đồng bộ nếu có
            if (coverImageUrl != null) {
                String publicId = cloudinaryService.getPublicIdFromUrl(coverImageUrl);
                if (publicId != null) {
                    asyncCloudinaryService.deleteImageAsync(publicId);
                }
            }
            // Upload ảnh mới bất đồng bộ
            asyncCloudinaryService.uploadStoryCoverImageAsync(storyId, request.getCoverImage());
        }

        // Cập nhật thông tin truyện
        story.setTitle(request.getTitle());
        story.setSlug(request.getSlug());
        story.setDescription(request.getDescription());
        story.setAuthorName(request.getAuthorName()); // THÊM DÒNG NÀY
        story.setStatus(request.getStatus());
        story.setCoverImageUrl(coverImageUrl);
        story.setGenres(genres);

        Story updatedStory = storyRepository.save(story);
        log.info("Cập nhật truyện thành công: storyId={}", updatedStory.getId());

        return storyMapper.toStoryResponse(updatedStory);
    }

    @Override
    public void deleteStory(Long storyId, Long userId) {
        log.info("Xóa truyện: storyId={}, userId={}", storyId, userId);

        // Lấy truyện và kiểm tra quyền
        Story story = getStoryById(storyId);
        validateDeletePermissions(story, userId);

        // Kiểm tra xem truyện có chapter hoặc user đã mua chưa
        Long chapterCount = storyRepository.countChaptersByStoryId(storyId);
        if (chapterCount > 0) {
            throw new IllegalStateException("Không thể xóa truyện đã có chapter. Số chapter: " + chapterCount);
        }

        // Xóa ảnh bìa bất đồng bộ nếu có
        if (story.getCoverImageUrl() != null) {
            String publicId = cloudinaryService.getPublicIdFromUrl(story.getCoverImageUrl());
            if (publicId != null) {
                asyncCloudinaryService.deleteImageAsync(publicId);
            }
        }

        // Xóa truyện (cascade sẽ xóa các bảng liên quan)
        storyRepository.delete(story);
        log.info("Xóa truyện thành công: storyId={}", storyId);
    }

    @Override
    @Transactional(readOnly = true)
    public StoryResponse getStoryDetail(String identifier, Long userId) {
        log.info("Lấy chi tiết truyện: identifier={}, userId={}", identifier, userId);

        Story story;
        try {
            // Thử parse thành ID
            Long storyId = Long.parseLong(identifier);
            story = getStoryById(storyId);
        } catch (NumberFormatException e) {
            // Nếu không phải số thì tìm theo slug
            story = getStoryBySlug(identifier);
        }

        // Kiểm tra quyền xem (nếu truyện bị ẩn)
        validateViewPermissions(story, userId);

        // Tăng view bất đồng bộ để tránh conflict với read-only transaction
        try {
            storyViewsService.increaseViewAsync(story.getId());
        } catch (Exception e) {
            log.warn("Không thể tăng views cho story {}: {}", story.getId(), e.getMessage());
        }

        return storyMapper.toStoryResponse(story);
    }

    @Override
    @Transactional(readOnly = true)
    public StoryListResponse searchStories(StorySearchRequest request) {
        log.info("Tìm kiếm truyện: search={}, genres={}, status={}",
                request.getSearch(), request.getGenreIds(), request.getStatus());

        // Xử lý search parameter
        String searchParam = null;
        if (request.getSearch() != null && !request.getSearch().trim().isEmpty()) {
            searchParam = request.getSearch().trim();
        }

        // Xử lý status parameter
        String statusParam = null;
        if (request.getStatus() != null) {
            statusParam = request.getStatus().name();
        }

        // Xử lý genreIds parameter
        Long[] genreIdsArray = new Long[0];
        int genreIdsSize = 0;
        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            genreIdsArray = request.getGenreIds().toArray(new Long[0]);
            genreIdsSize = genreIdsArray.length;
        }

        Page<Story> storyPage = storyRepository.searchAndFilterStories(
                searchParam,
                statusParam,
                genreIdsArray,
                genreIdsSize,
                request.toPageableWithoutSort());

        Page<StoryResponse> responsePage = storyPage.map(storyMapper::toStoryResponse);
        return StoryListResponse.fromPage(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public StoryListResponse getStoriesByAuthor(Long authorId, Pageable pageable) {
        log.info("Lấy danh sách truyện của tác giả: authorId={}", authorId);

        // Sử dụng JOIN FETCH để tránh lazy loading issues
        List<Story> stories = storyRepository.findByAuthorIdWithFetch(authorId);

        // Thực hiện phân trang thủ công
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), stories.size());

        if (start > stories.size()) {
            start = stories.size();
        }

        List<Story> pagedStories = stories.subList(start, end);

        // Chuyển đổi thành Page<StoryResponse>
        List<StoryResponse> storyResponses = pagedStories.stream()
                .map(storyMapper::toStoryResponse)
                .collect(Collectors.toList());

        StoryListResponse response = new StoryListResponse();
        response.setContent(storyResponses);
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        response.setTotalElements((long) stories.size());
        response.setTotalPages((int) Math.ceil((double) stories.size() / pageable.getPageSize()));
        response.setHasNext(pageable.getPageNumber() < response.getTotalPages() - 1);
        response.setHasPrevious(pageable.getPageNumber() > 0);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEditStory(Long storyId, Long userId) {
        try {
            Story story = getStoryById(storyId);
            return validateEditPermissions(story, userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlugExists(String slug) {
        return storyRepository.existsBySlug(slug);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlugExists(String slug, Long excludeStoryId) {
        return storyRepository.existsBySlugAndIdNot(slug, excludeStoryId);
    }

    // Helper methods
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + userId));
    }

    private Story getStoryById(Long storyId) {
        return storyRepository.findByIdWithFetch(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện: " + storyId));
    }

    private Story getStoryBySlug(String slug) {
        return storyRepository.findBySlugWithFetch(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện: " + slug));
    }

    private void validateAuthorPermissions(User author) {
        if (!author.getIsActive()) {
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }

        // Kiểm tra quyền tạo truyện (UPLOADER hoặc ADMIN)
        if (!securityUtils.hasAnyRole("UPLOADER", "ADMIN")) {
            throw new ForbiddenException("Không có quyền tạo truyện");
        }
    }

    private boolean validateEditPermissions(Story story, Long userId) {
        // Admin có thể sửa mọi truyện
        if (securityUtils.isAdmin()) {
            return true;
        }

        // Tác giả chỉ có thể sửa truyện của mình
        if (!story.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Không có quyền chỉnh sửa truyện này");
        }

        return true;
    }

    private void validateDeletePermissions(Story story, Long userId) {
        // Admin có thể xóa mọi truyện
        if (securityUtils.isAdmin()) {
            return;
        }

        // Tác giả chỉ có thể xóa truyện của mình
        if (!story.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Không có quyền xóa truyện này");
        }
    }

    private void validateViewPermissions(Story story, Long userId) {
        // TODO: Implement logic kiểm tra quyền xem truyện bị ẩn
        // Hiện tại cho phép xem tất cả truyện
    }

    private Set<Genre> validateAndGetGenres(Set<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất 1 thể loại");
        }

        Set<Genre> genres = new HashSet<>();
        for (Long genreId : genreIds) {
            Genre genre = genreRepository.findById(genreId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thể loại: " + genreId));
            genres.add(genre);
        }

        return genres;
    }

}