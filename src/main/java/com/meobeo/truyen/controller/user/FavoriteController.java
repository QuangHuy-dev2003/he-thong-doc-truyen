package com.meobeo.truyen.controller.user;

import com.meobeo.truyen.domain.request.favorite.AddToFavoriteRequest;
import com.meobeo.truyen.domain.response.favorite.FavoriteListResponse;
import com.meobeo.truyen.domain.response.favorite.FavoriteResponse;
import com.meobeo.truyen.service.interfaces.FavoriteService;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final SecurityUtils securityUtils;

    /**
     * POST /api/v1/favorites/add - Thêm truyện vào danh sách yêu thích
     */
    @PostMapping("/favorites/add")
    public ResponseEntity<ApiResponse<FavoriteResponse>> addToFavorite(
            @Valid @RequestBody AddToFavoriteRequest request) {

        log.info("API thêm truyện vào yêu thích được gọi: storyId={}", request.getStoryId());

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        FavoriteResponse favorite = favoriteService.addToFavorite(request.getStoryId(), userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã thêm truyện vào danh sách yêu thích thành công", favorite));
    }

    /**
     * DELETE /api/v1/favorites/remove/{storyId} - Xóa truyện khỏi danh sách yêu
     * thích
     */
    @DeleteMapping("/favorites/remove/{storyId}")
    public ResponseEntity<ApiResponse<Void>> removeFromFavorite(@PathVariable Long storyId) {

        log.info("API xóa truyện khỏi yêu thích được gọi: storyId={}", storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        favoriteService.removeFromFavorite(storyId, userId);

        return ResponseEntity.ok(ApiResponse.success("Đã xóa truyện khỏi danh sách yêu thích thành công", null));
    }

    /**
     * GET /api/v1/favorites/my-favorites - Lấy danh sách truyện yêu thích của user
     * hiện tại
     */
    @GetMapping("/favorites/my-favorites")
    public ResponseEntity<ApiResponse<FavoriteListResponse>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("API lấy danh sách yêu thích được gọi: page={}, size={}", page, size);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        PageRequest pageRequest = PageRequest.of(page, size);
        FavoriteListResponse favorites = favoriteService.getUserFavorites(userId, pageRequest);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu thích thành công", favorites));
    }

    /**
     * GET /api/v1/favorites/check/{storyId} - Kiểm tra user đã yêu thích truyện
     * chưa
     */
    @GetMapping("/favorites/check/{storyId}")
    public ResponseEntity<ApiResponse<Boolean>> checkFavoriteStatus(@PathVariable Long storyId) {

        log.info("API kiểm tra trạng thái yêu thích được gọi: storyId={}", storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        boolean isFavorite = favoriteService.isUserFavoriteStory(storyId, userId);

        return ResponseEntity.ok(ApiResponse.success("Kiểm tra trạng thái yêu thích thành công", isFavorite));
    }

    /**
     * GET /api/v1/favorites/count - Đếm số truyện yêu thích của user hiện tại
     */
    @GetMapping("/favorites/count")
    public ResponseEntity<ApiResponse<Long>> getMyFavoriteCount() {

        log.info("API đếm số yêu thích được gọi");

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        Long count = favoriteService.countUserFavorites(userId);

        return ResponseEntity.ok(ApiResponse.success("Đếm số yêu thích thành công", count));
    }

    /**
     * GET /api/v1/favorites/story/{storyId}/count - Đếm số người yêu thích truyện
     */
    @GetMapping("/favorites/story/{storyId}/count")
    public ResponseEntity<ApiResponse<Long>> getStoryFavoriteCount(@PathVariable Long storyId) {

        log.info("API đếm số người yêu thích truyện được gọi: storyId={}", storyId);

        Long count = favoriteService.countStoryFavorites(storyId);

        return ResponseEntity.ok(ApiResponse.success("Đếm số người yêu thích truyện thành công", count));
    }

    /**
     * GET /api/v1/favorites/user/{userId} - Lấy danh sách yêu thích của user khác
     * (public)
     */
    @GetMapping("/favorites/user/{userId}")
    public ResponseEntity<ApiResponse<FavoriteListResponse>> getUserFavorites(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("API lấy danh sách yêu thích của user khác được gọi: userId={}, page={}, size={}",
                userId, page, size);

        PageRequest pageRequest = PageRequest.of(page, size);
        FavoriteListResponse favorites = favoriteService.getUserFavorites(userId, pageRequest);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu thích thành công", favorites));
    }
}
