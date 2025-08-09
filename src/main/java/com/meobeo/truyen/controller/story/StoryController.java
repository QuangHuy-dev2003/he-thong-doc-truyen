package com.meobeo.truyen.controller.story;

import com.meobeo.truyen.domain.enums.StoryStatus;
import com.meobeo.truyen.domain.request.story.CreateStoryRequest;
import com.meobeo.truyen.domain.request.story.StorySearchRequest;
import com.meobeo.truyen.domain.request.story.UpdateStoryRequest;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.domain.response.story.StoryListResponse;
import com.meobeo.truyen.domain.response.story.StoryResponse;
import com.meobeo.truyen.service.interfaces.StoryService;
import com.meobeo.truyen.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class StoryController {

    private final StoryService storyService;
    private final SecurityUtils securityUtils;

    /**
     * POST /api/v1/stories/create - Tạo truyện mới
     */
    @PostMapping("/stories/create")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoryResponse>> createStory(
            @RequestParam("title") String title,
            @RequestParam("slug") String slug,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "authorName", required = false) String authorName,
            @RequestParam(value = "status", required = false) StoryStatus status,
            @RequestParam("genreIds") String genreIds,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage) {

        log.info("API tạo truyện mới được gọi");

        // Parse genreIds từ string sang Set<Long>
        Set<Long> parsedGenreIds = parseGenreIds(genreIds);

        // Tạo CreateStoryRequest
        CreateStoryRequest request = new CreateStoryRequest();
        request.setTitle(title);
        request.setSlug(slug);
        request.setDescription(description);
        request.setAuthorName(authorName);
        request.setStatus(status != null ? status : StoryStatus.ONGOING);
        request.setGenreIds(parsedGenreIds);
        request.setCoverImage(coverImage);

        Long authorId = securityUtils.getCurrentUserIdOrThrow();
        StoryResponse story = storyService.createStory(request, authorId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo truyện thành công", story));
    }

    /**
     * PUT /api/v1/stories/update/{id} - Cập nhật truyện
     */
    @PutMapping("/stories/update/{id}")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoryResponse>> updateStory(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("slug") String slug,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "authorName", required = false) String authorName,
            @RequestParam(value = "status", required = false) StoryStatus status,
            @RequestParam("genreIds") String genreIds,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage) {

        log.info("API cập nhật truyện được gọi: storyId={}", id);

        // Parse genreIds từ string sang Set<Long>
        Set<Long> parsedGenreIds = parseGenreIds(genreIds);

        // Tạo UpdateStoryRequest
        UpdateStoryRequest request = new UpdateStoryRequest();
        request.setTitle(title);
        request.setSlug(slug);
        request.setDescription(description);
        request.setAuthorName(authorName);
        request.setStatus(status);
        request.setGenreIds(parsedGenreIds);
        request.setCoverImage(coverImage);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        StoryResponse story = storyService.updateStory(id, request, userId);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật truyện thành công", story));
    }

    /**
     * DELETE /api/v1/stories/delete/{id} - Xóa truyện
     */
    @DeleteMapping("/stories/delete/{id}")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStory(@PathVariable Long id) {

        log.info("API xóa truyện được gọi: storyId={}", id);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        storyService.deleteStory(id, userId);

        return ResponseEntity.ok(ApiResponse.success("Xóa truyện thành công", null));
    }

    /**
     * GET /api/v1/stories/{id or slug} - Xem chi tiết truyện (Public)
     */
    @GetMapping("/stories/{identifier}")
    public ResponseEntity<ApiResponse<StoryResponse>> getStoryDetail(
            @PathVariable String identifier) {

        log.info("API xem chi tiết truyện được gọi: identifier={}", identifier);

        Long userId = securityUtils.getCurrentUserId().orElse(null);
        StoryResponse story = storyService.getStoryDetail(identifier, userId);

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin truyện thành công", story));
    }

    /**
     * GET /api/v1/stories - Tìm kiếm và lọc truyện (Public)
     */
    @GetMapping("/stories/filter")
    public ResponseEntity<ApiResponse<StoryListResponse>> searchStories(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "genreIds", required = false) String genreIds,
            @RequestParam(value = "status", required = false) StoryStatus status,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("API tìm kiếm truyện được gọi: search={}, genreIds={}, status={}",
                search, genreIds, status);

        // Tạo StorySearchRequest từ các tham số
        StorySearchRequest request = new StorySearchRequest();
        request.setSearch(search);
        request.setGenreIds(parseGenreIds(genreIds));
        request.setStatus(status);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);
        request.setPage(page);
        request.setSize(size);

        StoryListResponse stories = storyService.searchStories(request);

        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm truyện thành công", stories));
    }

    /**
     * GET /api/v1/stories/author/{authorId} - Lấy danh sách truyện của tác giả
     * (Public)
     */
    @GetMapping("/stories/author/{authorId}")
    public ResponseEntity<ApiResponse<StoryListResponse>> getStoriesByAuthor(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("API lấy truyện của tác giả được gọi: authorId={}, page={}, size={}", authorId, page, size);

        PageRequest pageRequest = PageRequest.of(page, size);
        StoryListResponse stories = storyService.getStoriesByAuthor(authorId, pageRequest);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách truyện của tác giả thành công", stories));
    }

    /**
     * GET /api/v1/stories/check-slug/{slug} - Kiểm tra slug đã tồn tại chưa
     * (Public)
     */
    @GetMapping("/stories/check-slug/{slug}")
    public ResponseEntity<ApiResponse<Boolean>> checkSlugExists(@PathVariable String slug) {

        log.info("API kiểm tra slug được gọi: slug={}", slug);

        boolean exists = storyService.isSlugExists(slug);

        return ResponseEntity.ok(ApiResponse.success("Kiểm tra slug thành công", exists));
    }

    /**
     * GET /api/v1/stories/{storyId}/can-edit - Kiểm tra quyền chỉnh sửa truyện
     */
    @GetMapping("/stories/{storyId}/can-edit")
    public ResponseEntity<ApiResponse<Boolean>> canEditStory(@PathVariable Long storyId) {

        log.info("API kiểm tra quyền chỉnh sửa được gọi: storyId={}", storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        boolean canEdit = storyService.canEditStory(storyId, userId);

        return ResponseEntity.ok(ApiResponse.success("Kiểm tra quyền chỉnh sửa thành công", canEdit));
    }

    /**
     * Parse genreIds từ string sang Set<Long>
     * Hỗ trợ format: "[1,2,3]" hoặc "1,2,3"
     */
    private Set<Long> parseGenreIds(String genreIds) {
        if (genreIds == null || genreIds.trim().isEmpty()) {
            return new HashSet<>();
        }

        String cleanGenreIds = genreIds.trim();
        // Xử lý trường hợp có dấu ngoặc vuông
        if (cleanGenreIds.startsWith("[") && cleanGenreIds.endsWith("]")) {
            cleanGenreIds = cleanGenreIds.substring(1, cleanGenreIds.length() - 1);
        }

        return Arrays.stream(cleanGenreIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }
}