package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.entity.StorySubscription;
import com.meobeo.truyen.domain.entity.StorySubscriptionId;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.response.subscription.SubscriptionListResponse;
import com.meobeo.truyen.domain.response.subscription.SubscriptionResponse;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.exception.UserAlreadyExistsException;
import com.meobeo.truyen.mapper.StorySubscriptionMapper;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.StorySubscriptionRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.StorySubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StorySubscriptionServiceImpl implements StorySubscriptionService {

    private final StorySubscriptionRepository storySubscriptionRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final StorySubscriptionMapper storySubscriptionMapper;

    @Override
    public SubscriptionResponse subscribeToStory(Long storyId, Long userId) {
        log.info("Đăng ký theo dõi truyện: storyId={}, userId={}", storyId, userId);

        // Kiểm tra user và story tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + userId));

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + storyId));

        // Kiểm tra đã đăng ký theo dõi chưa
        if (storySubscriptionRepository.existsByUserIdAndStoryId(userId, storyId)) {
            throw new UserAlreadyExistsException("Bạn đã đăng ký theo dõi truyện này rồi");
        }

        // Tạo subscription mới
        StorySubscription subscription = new StorySubscription();
        StorySubscriptionId subscriptionId = new StorySubscriptionId();
        subscriptionId.setUserId(userId);
        subscriptionId.setStoryId(storyId);
        subscription.setId(subscriptionId);
        subscription.setUser(user);
        subscription.setStory(story);
        subscription.setIsActive(true);

        StorySubscription savedSubscription = storySubscriptionRepository.save(subscription);
        log.info("Đã đăng ký theo dõi truyện thành công: storyId={}, userId={}", storyId, userId);

        return storySubscriptionMapper.toSubscriptionResponse(savedSubscription);
    }

    @Override
    public void unsubscribeFromStory(Long storyId, Long userId) {
        log.info("Hủy đăng ký theo dõi truyện: storyId={}, userId={}", storyId, userId);

        // Kiểm tra subscription tồn tại
        if (!storySubscriptionRepository.existsByUserIdAndStoryId(userId, storyId)) {
            throw new ResourceNotFoundException("Không tìm thấy đăng ký theo dõi truyện này");
        }

        storySubscriptionRepository.deleteByUserIdAndStoryId(userId, storyId);
        log.info("Đã hủy đăng ký theo dõi truyện thành công: storyId={}, userId={}", storyId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionListResponse getUserSubscriptions(Long userId, Pageable pageable) {
        log.info("Lấy danh sách đăng ký theo dõi của user: userId={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        // Kiểm tra user tồn tại
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy user với ID: " + userId);
        }

        Page<StorySubscription> subscriptionsPage = storySubscriptionRepository.findByUserIdWithFetch(userId, pageable);

        SubscriptionListResponse response = new SubscriptionListResponse();
        response.setContent(subscriptionsPage.getContent().stream()
                .map(storySubscriptionMapper::toSubscriptionResponse)
                .toList());
        response.setPage(subscriptionsPage.getNumber());
        response.setSize(subscriptionsPage.getSize());
        response.setTotalElements(subscriptionsPage.getTotalElements());
        response.setTotalPages(subscriptionsPage.getTotalPages());
        response.setHasNext(subscriptionsPage.hasNext());
        response.setHasPrevious(subscriptionsPage.hasPrevious());

        log.info("Đã lấy danh sách đăng ký theo dõi thành công: userId={}, total={}",
                userId, subscriptionsPage.getTotalElements());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserSubscribedToStory(Long storyId, Long userId) {
        return storySubscriptionRepository.isUserSubscribedToStory(userId, storyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countUserSubscriptions(Long userId) {
        return storySubscriptionRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countStorySubscribers(Long storyId) {
        return storySubscriptionRepository.countByStoryId(storyId);
    }

    @Override
    public SubscriptionResponse toggleSubscription(Long storyId, Long userId) {
        log.info("Toggle đăng ký theo dõi truyện: storyId={}, userId={}", storyId, userId);

        // Kiểm tra user và story tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + userId));

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + storyId));

        // Kiểm tra đã đăng ký theo dõi chưa
        if (storySubscriptionRepository.existsByUserIdAndStoryId(userId, storyId)) {
            // Nếu đã đăng ký thì hủy
            storySubscriptionRepository.deleteByUserIdAndStoryId(userId, storyId);
            log.info("Đã hủy đăng ký theo dõi truyện: storyId={}, userId={}", storyId, userId);
            return null; // Trả về null để client biết đã hủy
        } else {
            // Nếu chưa đăng ký thì đăng ký
            StorySubscription subscription = new StorySubscription();
            StorySubscriptionId subscriptionId = new StorySubscriptionId();
            subscriptionId.setUserId(userId);
            subscriptionId.setStoryId(storyId);
            subscription.setId(subscriptionId);
            subscription.setUser(user);
            subscription.setStory(story);
            subscription.setIsActive(true);

            StorySubscription savedSubscription = storySubscriptionRepository.save(subscription);
            log.info("Đã đăng ký theo dõi truyện: storyId={}, userId={}", storyId, userId);

            return storySubscriptionMapper.toSubscriptionResponse(savedSubscription);
        }
    }
}
