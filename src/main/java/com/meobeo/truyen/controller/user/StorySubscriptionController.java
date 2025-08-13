package com.meobeo.truyen.controller.user;

import com.meobeo.truyen.domain.request.subscription.AddSubscriptionRequest;
import com.meobeo.truyen.domain.response.subscription.SubscriptionListResponse;
import com.meobeo.truyen.domain.response.subscription.SubscriptionResponse;
import com.meobeo.truyen.service.interfaces.StorySubscriptionService;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class StorySubscriptionController {

    private final StorySubscriptionService storySubscriptionService;
    private final SecurityUtils securityUtils;

    /**
     * POST /api/v1/subscriptions/add - Đăng ký theo dõi truyện
     */
    @PostMapping("/subscriptions/add")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribeToStory(
            @Valid @RequestBody AddSubscriptionRequest request) {

        log.info("API đăng ký theo dõi truyện được gọi: storyId={}", request.getStoryId());

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        SubscriptionResponse subscription = storySubscriptionService.subscribeToStory(request.getStoryId(), userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã đăng ký theo dõi truyện thành công", subscription));
    }

    /**
     * DELETE /api/v1/subscriptions/remove/{storyId} - Hủy đăng ký theo dõi truyện
     */
    @DeleteMapping("/subscriptions/remove/{storyId}")
    public ResponseEntity<ApiResponse<Void>> unsubscribeFromStory(@PathVariable Long storyId) {

        log.info("API hủy đăng ký theo dõi truyện được gọi: storyId={}", storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        storySubscriptionService.unsubscribeFromStory(storyId, userId);

        return ResponseEntity.ok(ApiResponse.success("Đã hủy đăng ký theo dõi truyện thành công", null));
    }

    /**
     * POST /api/v1/subscriptions/toggle/{storyId} - Toggle đăng ký theo dõi truyện
     */
    @PostMapping("/subscriptions/toggle/{storyId}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> toggleSubscription(@PathVariable Long storyId) {

        log.info("API toggle đăng ký theo dõi truyện được gọi: storyId={}", storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        SubscriptionResponse subscription = storySubscriptionService.toggleSubscription(storyId, userId);

        if (subscription != null) {
            return ResponseEntity.ok(ApiResponse.success("Đã đăng ký theo dõi truyện thành công", subscription));
        } else {
            return ResponseEntity.ok(ApiResponse.success("Đã hủy đăng ký theo dõi truyện thành công", null));
        }
    }

    /**
     * GET /api/v1/subscriptions/my-subscriptions - Lấy danh sách truyện đang theo
     * dõi của user hiện tại
     */
    @GetMapping("/subscriptions/my-subscriptions")
    public ResponseEntity<ApiResponse<SubscriptionListResponse>> getMySubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("API lấy danh sách đăng ký theo dõi được gọi: page={}, size={}", page, size);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        PageRequest pageRequest = PageRequest.of(page, size);
        SubscriptionListResponse subscriptions = storySubscriptionService.getUserSubscriptions(userId, pageRequest);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đăng ký theo dõi thành công", subscriptions));
    }

    /**
     * GET /api/v1/subscriptions/check/{storyId} - Kiểm tra user đã đăng ký theo dõi
     * truyện chưa
     */
    @GetMapping("/subscriptions/check/{storyId}")
    public ResponseEntity<ApiResponse<Boolean>> checkSubscriptionStatus(@PathVariable Long storyId) {

        log.info("API kiểm tra trạng thái đăng ký theo dõi được gọi: storyId={}", storyId);

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        boolean isSubscribed = storySubscriptionService.isUserSubscribedToStory(storyId, userId);

        return ResponseEntity.ok(ApiResponse.success("Kiểm tra trạng thái đăng ký theo dõi thành công", isSubscribed));
    }

    /**
     * GET /api/v1/subscriptions/count - Đếm số truyện đang theo dõi của user hiện
     * tại
     */
    @GetMapping("/subscriptions/count")
    public ResponseEntity<ApiResponse<Long>> getMySubscriptionCount() {

        log.info("API đếm số đăng ký theo dõi được gọi");

        Long userId = securityUtils.getCurrentUserIdOrThrow();
        Long count = storySubscriptionService.countUserSubscriptions(userId);

        return ResponseEntity.ok(ApiResponse.success("Đếm số đăng ký theo dõi thành công", count));
    }

    /**
     * GET /api/v1/subscriptions/story/{storyId}/count - Đếm số người đăng ký theo
     * dõi truyện
     */
    @GetMapping("/subscriptions/story/{storyId}/count")
    public ResponseEntity<ApiResponse<Long>> getStorySubscriberCount(@PathVariable Long storyId) {

        log.info("API đếm số người đăng ký theo dõi truyện được gọi: storyId={}", storyId);

        Long count = storySubscriptionService.countStorySubscribers(storyId);

        return ResponseEntity.ok(ApiResponse.success("Đếm số người đăng ký theo dõi truyện thành công", count));
    }

    /**
     * GET /api/v1/subscriptions/user/{userId} - Lấy danh sách đăng ký theo dõi của
     * user khác (public)
     */
    @GetMapping("/subscriptions/user/{userId}")
    public ResponseEntity<ApiResponse<SubscriptionListResponse>> getUserSubscriptions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("API lấy danh sách đăng ký theo dõi của user khác được gọi: userId={}, page={}, size={}",
                userId, page, size);

        PageRequest pageRequest = PageRequest.of(page, size);
        SubscriptionListResponse subscriptions = storySubscriptionService.getUserSubscriptions(userId, pageRequest);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đăng ký theo dõi thành công", subscriptions));
    }
}
