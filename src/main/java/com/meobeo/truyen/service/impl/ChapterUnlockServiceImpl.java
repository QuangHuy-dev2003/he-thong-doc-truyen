package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.*;
import com.meobeo.truyen.domain.enums.TransactionType;
import com.meobeo.truyen.domain.repository.ChapterUnlockRepository;
import com.meobeo.truyen.repository.ChapterPaymentRepository;
import com.meobeo.truyen.repository.ChapterRepository;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.UserWalletRepository;
import com.meobeo.truyen.repository.WalletTransactionRepository;
import com.meobeo.truyen.domain.request.chapter.UnlockChapterRangeRequest;
import com.meobeo.truyen.domain.request.chapter.UnlockFullStoryRequest;
import com.meobeo.truyen.domain.response.chapter.UnlockChapterBatchResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockChapterResponse;
import com.meobeo.truyen.domain.response.chapter.UnlockFullStoryResponse;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.InsufficientBalanceException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.service.interfaces.AsyncChapterUnlockService;
import com.meobeo.truyen.service.interfaces.ChapterUnlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterUnlockServiceImpl implements ChapterUnlockService {

    private final ChapterUnlockRepository chapterUnlockRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPaymentRepository chapterPaymentRepository;
    private final StoryRepository storyRepository;
    private final UserWalletRepository userWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AsyncChapterUnlockService asyncChapterUnlockService;

    // Constants cho discount
    private static final double BATCH_DISCOUNT_PERCENT = 0.02; // 2% cho >200 chương
    private static final double FULL_UNLOCK_DISCOUNT_PERCENT = 0.10; // 10% cho mở full
    private static final int BATCH_DISCOUNT_THRESHOLD = 200; // Ngưỡng áp dụng discount

    @Override
    @Transactional
    public UnlockChapterResponse unlockChapter(Long chapterId, Long userId) {
        log.info("Bắt đầu mở khóa chương {} cho user {}", chapterId, userId);

        // Validation
        Chapter chapter = validateChapterExists(chapterId);
        validateChapterIsLocked(chapter);
        validateUserHasNotUnlocked(userId, chapterId);

        // Lấy thông tin giá
        ChapterPayment payment = chapterPaymentRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin giá chương"));

        int originalPrice = payment.getPrice();
        int finalPrice = calculateUnlockPrice(originalPrice, 1, false);

        // Kiểm tra số dư
        UserWallet wallet = getUserWallet(userId);
        validateSufficientBalance(wallet, finalPrice);

        // Thực hiện mở khóa
        ChapterUnlock unlock = performUnlock(userId, chapter, finalPrice, null,
                TransactionType.CHAPTER_UNLOCK_SPIRIT_STONE);

        // Tạo response
        UnlockChapterResponse response = new UnlockChapterResponse();
        response.setChapterId(chapter.getId());
        response.setChapterTitle(chapter.getTitle());
        response.setSpiritStonesSpent(finalPrice);
        response.setOriginalPrice(originalPrice);
        response.setDiscountedPrice(finalPrice);
        response.setDiscountPercent(0.0); // Không có discount cho 1 chương
        response.setDescription(null);
        response.setUnlockedAt(unlock.getUnlockedAt());

        log.info("Mở khóa chương {} thành công cho user {}, tiêu {} linh thạch",
                chapter.getId(), userId, finalPrice);

        return response;
    }

    @Override
    @Transactional
    public UnlockChapterBatchResponse unlockChapterRange(Long storyId, UnlockChapterRangeRequest request, Long userId) {
        log.info("Bắt đầu mở khóa chương từ {} đến {} cho story {} của user {}",
                request.getFromChapterNumber(), request.getToChapterNumber(), storyId, userId);

        // Validation
        Story story = validateStoryExists(storyId);
        validateChapterRange(request.getFromChapterNumber(), request.getToChapterNumber());

        // Lấy danh sách chapter trong range
        List<Chapter> chapters = chapterRepository.findByStoryIdAndChapterNumberBetweenOrderByChapterNumber(
                storyId, request.getFromChapterNumber(), request.getToChapterNumber());

        if (chapters.isEmpty()) {
            throw new BadRequestException("Không tìm thấy chương nào trong khoảng đã chọn");
        }

        // Lọc chapter bị khóa và chưa mở khóa
        List<Chapter> lockedChapters = chapters.stream()
                .filter(chapter -> isChapterLocked(chapter.getId()))
                .filter(chapter -> !hasUserUnlockedChapter(userId, chapter.getId()))
                .collect(Collectors.toList());

        if (lockedChapters.isEmpty()) {
            throw new BadRequestException("Tất cả chương trong khoảng đã được mở khóa hoặc không bị khóa");
        }

        // Tính tổng giá
        int totalOriginalPrice = 0;

        for (Chapter chapter : lockedChapters) {
            ChapterPayment payment = chapterPaymentRepository.findById(chapter.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy thông tin giá chương " + chapter.getId()));

            int originalPrice = payment.getPrice();
            totalOriginalPrice += originalPrice;
        }

        int totalFinalPrice = calculateUnlockPrice(totalOriginalPrice, lockedChapters.size(), false);

        // Kiểm tra số dư
        UserWallet wallet = getUserWallet(userId);
        validateSufficientBalance(wallet, totalFinalPrice);

        // Thực hiện mở khóa batch
        LocalDateTime unlockedAt = LocalDateTime.now();
        for (Chapter chapter : lockedChapters) {
            ChapterPayment payment = chapterPaymentRepository.findById(chapter.getId()).orElse(null);
            if (payment != null) {
                int originalPrice = payment.getPrice();
                int discountedPrice = calculateUnlockPrice(originalPrice, lockedChapters.size(), false);

                performUnlock(userId, chapter, discountedPrice, null,
                        TransactionType.CHAPTER_UNLOCK_BATCH_SPIRIT_STONE);
            }
        }

        // Tạo response
        UnlockChapterBatchResponse response = new UnlockChapterBatchResponse();
        response.setStoryId(story.getId());
        response.setStoryTitle(story.getTitle());
        response.setTotalChaptersUnlocked(lockedChapters.size());
        response.setTotalSpiritStonesSpent(totalFinalPrice);
        response.setTotalOriginalPrice(totalOriginalPrice);
        response.setTotalDiscountedPrice(totalFinalPrice);
        response.setTotalDiscountPercent(
                lockedChapters.size() > BATCH_DISCOUNT_THRESHOLD ? BATCH_DISCOUNT_PERCENT * 100 : 0.0);
        response.setUnlockedAt(unlockedAt);

        log.info("Mở khóa {} chương thành công cho story {} của user {}, tiêu {} linh thạch",
                lockedChapters.size(), story.getId(), userId, totalFinalPrice);

        return response;
    }

    @Override
    @Transactional
    public UnlockFullStoryResponse unlockFullStory(Long storyId, UnlockFullStoryRequest request, Long userId) {
        log.info("Bắt đầu mở khóa full truyện {} cho user {}", storyId, userId);

        // Validation
        Story story = validateStoryExists(storyId);

        // Lấy tất cả chapter của story
        List<Chapter> allChapters = getAllChaptersByStoryId(storyId);

        if (allChapters.isEmpty()) {
            throw new BadRequestException("Truyện không có chương nào");
        }

        // Lọc chapter bị khóa và chưa mở khóa
        List<Chapter> lockedChapters = allChapters.stream()
                .filter(chapter -> isChapterLocked(chapter.getId()))
                .filter(chapter -> !hasUserUnlockedChapter(userId, chapter.getId()))
                .collect(Collectors.toList());

        if (lockedChapters.isEmpty()) {
            throw new BadRequestException("Tất cả chương đã được mở khóa hoặc không bị khóa");
        }

        // Tính tổng giá
        int totalOriginalPrice = 0;
        for (Chapter chapter : lockedChapters) {
            ChapterPayment payment = chapterPaymentRepository.findById(chapter.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy thông tin giá chương " + chapter.getId()));
            totalOriginalPrice += payment.getPrice();
        }

        int totalFinalPrice = calculateUnlockPrice(totalOriginalPrice, lockedChapters.size(), true);

        // Kiểm tra số dư
        UserWallet wallet = getUserWallet(userId);
        validateSufficientBalance(wallet, totalFinalPrice);

        // Thực hiện mở khóa full
        LocalDateTime unlockedAt = LocalDateTime.now();
        for (Chapter chapter : lockedChapters) {
            ChapterPayment payment = chapterPaymentRepository.findById(chapter.getId()).orElse(null);
            if (payment != null) {
                int originalPrice = payment.getPrice();
                int discountedPrice = calculateUnlockPrice(originalPrice, lockedChapters.size(), true);

                performUnlock(userId, chapter, discountedPrice, null,
                        TransactionType.CHAPTER_UNLOCK_FULL_SPIRIT_STONE);
            }
        }

        // Tạo response
        UnlockFullStoryResponse response = new UnlockFullStoryResponse();
        response.setStoryId(story.getId());
        response.setStoryTitle(story.getTitle());
        response.setTotalChaptersUnlocked(lockedChapters.size());
        response.setTotalSpiritStonesSpent(totalFinalPrice);
        response.setTotalOriginalPrice(totalOriginalPrice);
        response.setTotalDiscountedPrice(totalFinalPrice);
        response.setTotalDiscountPercent(FULL_UNLOCK_DISCOUNT_PERCENT * 100);
        response.setUnlockedAt(unlockedAt);

        log.info("Mở khóa full truyện {} thành công cho user {}, mở {} chương, tiêu {} linh thạch",
                story.getId(), userId, lockedChapters.size(), totalFinalPrice);

        return response;
    }

    @Override
    public boolean isChapterLocked(Long chapterId) {
        ChapterPayment payment = chapterPaymentRepository.findById(chapterId).orElse(null);
        return payment != null && Boolean.TRUE.equals(payment.getIsLocked());
    }

    @Override
    public boolean hasUserUnlockedChapter(Long userId, Long chapterId) {
        return chapterUnlockRepository.existsByUserIdAndChapterId(userId, chapterId);
    }

    @Override
    public List<Long> getUserUnlockedChapterIds(Long userId, Long storyId) {
        return chapterUnlockRepository.findUnlockedChapterIdsByUserAndStory(userId, storyId);
    }

    @Override
    public Page<Long> getUserUnlockedChapterIds(Long userId, Long storyId, Pageable pageable) {
        return chapterUnlockRepository.findUnlockedChapterIdsByUserAndStory(userId, storyId, pageable);
    }

    @Override
    public int calculateUnlockPrice(int originalPrice, int chapterCount, boolean isFullUnlock) {
        double discountPercent = 0.0;

        if (isFullUnlock) {
            discountPercent = FULL_UNLOCK_DISCOUNT_PERCENT;
        } else if (chapterCount > BATCH_DISCOUNT_THRESHOLD) {
            discountPercent = BATCH_DISCOUNT_PERCENT;
        }

        return (int) Math.round(originalPrice * (1 - discountPercent));
    }

    // Helper methods
    private Chapter validateChapterExists(Long chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương với ID: " + chapterId));
    }

    private void validateChapterIsLocked(Chapter chapter) {
        if (!isChapterLocked(chapter.getId())) {
            throw new BadRequestException("Chương không bị khóa");
        }
    }

    private void validateUserHasNotUnlocked(Long userId, Long chapterId) {
        if (hasUserUnlockedChapter(userId, chapterId)) {
            throw new BadRequestException("Bạn đã mở khóa chương này rồi");
        }
    }

    private void validateChapterRange(Integer fromChapter, Integer toChapter) {
        if (fromChapter > toChapter) {
            throw new BadRequestException("Số chương bắt đầu phải nhỏ hơn hoặc bằng số chương kết thúc");
        }
    }

    private UserWallet getUserWallet(Long userId) {
        return userWalletRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của user: " + userId));
    }

    private void validateSufficientBalance(UserWallet wallet, int requiredAmount) {
        if (wallet.getSpiritStones() < requiredAmount) {
            throw new InsufficientBalanceException("Số dư linh thạch không đủ. Cần: " + requiredAmount +
                    ", Hiện có: " + wallet.getSpiritStones());
        }
    }

    private ChapterUnlock performUnlock(Long userId, Chapter chapter, int spiritStonesSpent,
            String description, TransactionType transactionType) {
        // Trừ linh thạch trước
        UserWallet wallet = getUserWallet(userId);
        wallet.setSpiritStones(wallet.getSpiritStones() - spiritStonesSpent);
        userWalletRepository.save(wallet);

        // Tạo ChapterUnlock record
        ChapterUnlockId unlockId = new ChapterUnlockId();
        unlockId.setUserId(userId);
        unlockId.setChapterId(chapter.getId());

        ChapterUnlock unlock = new ChapterUnlock();
        unlock.setId(unlockId);
        unlock.setUser(wallet.getUser()); // Lấy user thực tế từ wallet
        unlock.setChapter(chapter);
        unlock.setUnlockedAt(LocalDateTime.now());

        chapterUnlockRepository.save(unlock);

        // Tạo transaction record
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUser(wallet.getUser());
        transaction.setAmount(-spiritStonesSpent);
        transaction.setType(transactionType);
        transaction.setCurrency(WalletTransaction.CurrencyType.SPIRIT_STONE);
        transaction.setDescription(description != null ? description
                : "Mở khóa chương " + chapter.getTitle() + " của truyện " + chapter.getStory().getTitle());

        walletTransactionRepository.save(transaction);

        return unlock;
    }

    // Helper method để lấy tất cả chapter của story
    private List<Chapter> getAllChaptersByStoryId(Long storyId) {
        return chapterRepository.findByStoryIdAndChapterNumberBetweenOrderByChapterNumber(storyId, 1,
                Integer.MAX_VALUE);
    }

    // Helper method để validate story exists
    private Story validateStoryExists(Long storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + storyId));
    }

    // Async methods
    @Override
    public String startAsyncUnlockRange(Long storyId, UnlockChapterRangeRequest request, Long userId) {
        String jobId = java.util.UUID.randomUUID().toString();
        log.info("Khởi tạo async unlock range: jobId={}, storyId={}, userId={}",
                jobId, storyId, userId);

        // Validation range size cho async
        int rangeSize = request.getToChapterNumber() - request.getFromChapterNumber() + 1;
        if (rangeSize < 20) {
            throw new BadRequestException("Async chỉ dành cho range >= 20 chapter");
        }

        // Khởi tạo job tracking trong async service
        asyncChapterUnlockService.initializeRangeJob(jobId, userId);

        // Gọi async service để xử lý bất đồng bộ
        asyncChapterUnlockService.processUnlockRangeAsync(storyId, request, userId, jobId);

        log.info("Đã trả về jobId ngay lập tức: {}", jobId);
        return jobId;
    }

    @Override
    public String startAsyncUnlockFullStory(Long storyId, UnlockFullStoryRequest request, Long userId) {
        String jobId = java.util.UUID.randomUUID().toString();
        log.info("Khởi tạo async unlock full story: jobId={}, storyId={}, userId={}",
                jobId, storyId, userId);

        // Khởi tạo job tracking trong async service
        asyncChapterUnlockService.initializeFullStoryJob(jobId, userId);

        // Gọi async service để xử lý bất đồng bộ
        asyncChapterUnlockService.processUnlockFullStoryAsync(storyId, request, userId, jobId);

        log.info("Đã trả về jobId ngay lập tức: {}", jobId);
        return jobId;
    }

    @Override
    public java.util.Optional<UnlockChapterBatchResponse> getAsyncUnlockRangeStatus(String jobId) {
        return java.util.Optional.ofNullable(asyncChapterUnlockService.getUnlockRangeJobs().get(jobId));
    }

    @Override
    public java.util.Optional<UnlockFullStoryResponse> getAsyncUnlockFullStoryStatus(String jobId) {
        return java.util.Optional.ofNullable(asyncChapterUnlockService.getUnlockFullStoryJobs().get(jobId));
    }

    @Override
    public boolean cancelAsyncUnlockRange(String jobId, Long userId) {
        // Kiểm tra quyền
        Long jobUserId = asyncChapterUnlockService.getJobUserMap().get(jobId);
        if (jobUserId == null || !jobUserId.equals(userId)) {
            return false;
        }

        AtomicBoolean cancelFlag = asyncChapterUnlockService.getCancelFlags().get(jobId);
        if (cancelFlag != null) {
            cancelFlag.set(true);
            log.info("Đã hủy job unlock range: jobId={}, userId={}", jobId, userId);
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelAsyncUnlockFullStory(String jobId, Long userId) {
        // Kiểm tra quyền
        Long jobUserId = asyncChapterUnlockService.getJobUserMap().get(jobId);
        if (jobUserId == null || !jobUserId.equals(userId)) {
            return false;
        }

        AtomicBoolean cancelFlag = asyncChapterUnlockService.getCancelFlags().get(jobId);
        if (cancelFlag != null) {
            cancelFlag.set(true);
            log.info("Đã hủy job unlock full story: jobId={}, userId={}", jobId, userId);
            return true;
        }
        return false;
    }

    @Override
    public UnlockFullStoryResponse checkUnlockFullStoryStatus(Long storyId, Long userId) {
        log.info("Check trạng thái unlock full truyện: storyId={}, userId={}", storyId, userId);

        // Validation
        Story story = validateStoryExists(storyId);

        // Đếm tổng số chapter cần unlock (tối ưu query)
        Long totalChaptersToUnlock = chapterPaymentRepository.countChaptersToUnlockByStory(storyId, userId);

        // Tính tổng giá
        int totalOriginalPrice = calculateTotalPriceForUnlock(storyId, userId);
        int totalFinalPrice = calculateUnlockPrice(totalOriginalPrice, totalChaptersToUnlock.intValue(), true);

        // Tạo response
        UnlockFullStoryResponse response = new UnlockFullStoryResponse();
        response.setStoryId(story.getId());
        response.setStoryTitle(story.getTitle());
        response.setTotalChaptersToUnlock(totalChaptersToUnlock.intValue());
        response.setTotalChaptersUnlocked(0);
        response.setTotalOriginalPrice(totalOriginalPrice);
        response.setTotalDiscountedPrice(totalFinalPrice);
        response.setTotalSpiritStonesSpent(totalFinalPrice);
        response.setTotalDiscountPercent(FULL_UNLOCK_DISCOUNT_PERCENT * 100);
        response.setStatus("READY");
        response.setMessage(totalChaptersToUnlock > 0 ? "Sẵn sàng mở khóa " + totalChaptersToUnlock + " chương"
                : "Tất cả chương đã được mở khóa hoặc không bị khóa");

        log.info("Check trạng thái unlock full truyện thành công: storyId={}, chaptersToUnlock={}, price={}",
                storyId, totalChaptersToUnlock, totalFinalPrice);

        return response;
    }

    /**
     * Tính tổng giá cho việc unlock (tối ưu query)
     */
    private int calculateTotalPriceForUnlock(Long storyId, Long userId) {
        int totalPrice = 0;
        int offset = 0;
        int limit = 100; // Lấy từng batch 100 chapter để tính giá

        while (true) {
            List<Object[]> chapters = chapterPaymentRepository.findChaptersToUnlockByStory(storyId, userId, limit,
                    offset);
            if (chapters.isEmpty()) {
                break;
            }

            for (Object[] chapter : chapters) {
                Integer price = (Integer) chapter[1]; // price ở index 1
                totalPrice += price;
            }

            offset += limit;
        }

        return totalPrice;
    }
}
