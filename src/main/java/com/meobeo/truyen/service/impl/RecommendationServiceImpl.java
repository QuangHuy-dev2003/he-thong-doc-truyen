package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.entity.StoryRecommendation;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.entity.UserWallet;
import com.meobeo.truyen.domain.entity.WalletTransaction;
import com.meobeo.truyen.domain.enums.TransactionType;
import com.meobeo.truyen.domain.mapper.RecommendationMapper;
import com.meobeo.truyen.domain.repository.StoryRecommendationRepository;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.repository.UserWalletRepository;
import com.meobeo.truyen.repository.WalletTransactionRepository;
import com.meobeo.truyen.domain.request.recommendation.CreateRecommendationRequest;
import com.meobeo.truyen.domain.response.recommendation.RecommendationListResponse;
import com.meobeo.truyen.domain.response.recommendation.RecommendationResponse;
import com.meobeo.truyen.domain.response.recommendation.TopRecommendedStoriesResponse;
import com.meobeo.truyen.domain.response.story.StoryResponse;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.InsufficientBalanceException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.exception.UserNotFoundException;
import com.meobeo.truyen.mapper.StoryMapper;
import com.meobeo.truyen.service.interfaces.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final StoryRecommendationRepository storyRecommendationRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final UserWalletRepository userWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final RecommendationMapper recommendationMapper;
    private final StoryMapper storyMapper;

    // Rate limiting: tối đa 10 đề cử/ngày/user
    private static final int MAX_RECOMMENDATIONS_PER_DAY = 10;

    @Override
    @Transactional
    public RecommendationResponse createRecommendation(CreateRecommendationRequest request, Long userId) {
        log.info("User {} tạo đề cử cho story {}", userId, request.getStoryId());

        // Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));

        // Kiểm tra story tồn tại và đang active
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện"));

        if (story.getStatus() == null || story.getStatus().toString().equals("INACTIVE")) {
            throw new BadRequestException("Truyện không còn hoạt động");
        }

        // Kiểm tra user không đề cử story của chính mình
        if (story.getAuthor().getId().equals(userId)) {
            throw new BadRequestException("Không thể đề cử truyện của chính mình");
        }

        // Kiểm tra user đã đề cử story này chưa
        if (storyRecommendationRepository.existsByUserIdAndStoryId(userId, request.getStoryId())) {
            throw new BadRequestException("Bạn đã đề cử truyện này rồi");
        }

        // Kiểm tra rate limiting
        long todayRecommendations = storyRecommendationRepository.countByUserIdAndCreatedAtToday(userId);
        if (todayRecommendations >= MAX_RECOMMENDATIONS_PER_DAY) {
            throw new BadRequestException("Bạn đã đạt giới hạn đề cử trong ngày (" + MAX_RECOMMENDATIONS_PER_DAY + ")");
        }

        // Kiểm tra số phiếu đề cử
        UserWallet userWallet = userWalletRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của người dùng"));

        if (userWallet.getRecommendationTickets() < 1) {
            throw new InsufficientBalanceException(
                    "Không đủ phiếu đề cử. Cần: 1, Hiện có: " + userWallet.getRecommendationTickets());
        }

        // Trừ phiếu đề cử
        userWallet.setRecommendationTickets(userWallet.getRecommendationTickets() - 1);
        userWalletRepository.save(userWallet);

        // Tạo giao dịch ví
        WalletTransaction transaction = new WalletTransaction();
        transaction.setAmount(-1);
        transaction.setCurrency(WalletTransaction.CurrencyType.RECOMMENDATION_TICKET);
        transaction.setType(TransactionType.RECOMMENDATION_TICKET_SPEND);
        transaction.setDescription("Tiêu phiếu đề cử cho truyện: " + story.getTitle());
        transaction.setUser(user);
        walletTransactionRepository.save(transaction);

        // Tạo đề cử
        StoryRecommendation recommendation = recommendationMapper.toEntity(request, userId);
        StoryRecommendation savedRecommendation = storyRecommendationRepository.save(recommendation);

        // Fetch đầy đủ thông tin để trả về
        savedRecommendation.setUser(user);
        savedRecommendation.setStory(story);

        log.info("Tạo đề cử thành công. User: {}, Story: {}, Type: {}",
                userId, story.getTitle(), request.getRecommendationType());

        return recommendationMapper.toResponse(savedRecommendation);
    }

    @Override
    public RecommendationListResponse getUserRecommendations(Long userId, Pageable pageable) {
        log.info("Lấy danh sách đề cử của user {}", userId);

        Page<StoryRecommendation> recommendations = storyRecommendationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<RecommendationResponse> responses = recommendationMapper.toResponseList(recommendations.getContent());

        RecommendationListResponse result = new RecommendationListResponse();
        result.setRecommendations(responses);
        result.setPage(recommendations.getNumber());
        result.setSize(recommendations.getSize());
        result.setTotalElements(recommendations.getTotalElements());
        result.setTotalPages(recommendations.getTotalPages());

        return result;
    }

    @Override
    public RecommendationListResponse getStoryRecommendations(Long storyId, Pageable pageable) {
        log.info("Lấy danh sách đề cử của story {}", storyId);

        Page<StoryRecommendation> recommendations = storyRecommendationRepository
                .findByStoryIdOrderByCreatedAtDesc(storyId, pageable);

        List<RecommendationResponse> responses = recommendationMapper.toResponseList(recommendations.getContent());

        RecommendationListResponse result = new RecommendationListResponse();
        result.setRecommendations(responses);
        result.setPage(recommendations.getNumber());
        result.setSize(recommendations.getSize());
        result.setTotalElements(recommendations.getTotalElements());
        result.setTotalPages(recommendations.getTotalPages());

        return result;
    }

    @Override
    public TopRecommendedStoriesResponse getTopRecommendedStories(Pageable pageable) {
        log.info("Lấy top stories được đề cử nhiều nhất");

        List<Object[]> topStoriesData = storyRecommendationRepository
                .findTopStoriesWithRecommendationCount(pageable);

        List<Long> storyIds = topStoriesData.stream()
                .map(data -> (Long) data[0])
                .collect(Collectors.toList());

        List<Long> recommendationCounts = topStoriesData.stream()
                .map(data -> (Long) data[1])
                .collect(Collectors.toList());

        // Lấy thông tin đầy đủ của stories
        List<Story> stories = storyRepository.findAllById(storyIds);
        List<StoryResponse> storyResponses = storyMapper.toResponseList(stories);

        TopRecommendedStoriesResponse result = new TopRecommendedStoriesResponse();
        result.setStories(storyResponses);
        result.setRecommendationCounts(recommendationCounts);
        result.setPage(pageable.getPageNumber());
        result.setSize(pageable.getPageSize());
        result.setTotalElements(storyRecommendationRepository.count());
        result.setTotalPages((int) Math.ceil((double) storyRecommendationRepository.count() / pageable.getPageSize()));

        return result;
    }

    @Override
    public long getRecommendationCount(Long storyId) {
        return storyRecommendationRepository.countByStoryId(storyId);
    }

    @Override
    public boolean hasUserRecommendedStory(Long userId, Long storyId) {
        return storyRecommendationRepository.existsByUserIdAndStoryId(userId, storyId);
    }
}
