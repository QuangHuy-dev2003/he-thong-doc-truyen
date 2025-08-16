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
import com.meobeo.truyen.domain.response.chapter.UnlockFullStoryResponse;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.InsufficientBalanceException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.service.interfaces.AsyncChapterUnlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncChapterUnlockServiceImpl implements AsyncChapterUnlockService {

    private final ChapterUnlockRepository chapterUnlockRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPaymentRepository chapterPaymentRepository;
    private final StoryRepository storyRepository;
    private final UserWalletRepository userWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    // Cache để lưu trạng thái các job đang chạy
    private final Map<String, UnlockChapterBatchResponse> unlockRangeJobs = new ConcurrentHashMap<>();
    private final Map<String, UnlockFullStoryResponse> unlockFullStoryJobs = new ConcurrentHashMap<>();

    // Map để lưu userId của từng job
    private final Map<String, Long> jobUserMap = new ConcurrentHashMap<>();

    // Flag để hủy job
    private final Map<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    // Constants cho discount
    private static final double BATCH_DISCOUNT_PERCENT = 0.02; // 2% cho >200 chương
    private static final double FULL_UNLOCK_DISCOUNT_PERCENT = 0.10; // 10% cho mở full
    private static final int BATCH_DISCOUNT_THRESHOLD = 200; // Ngưỡng áp dụng discount
    private static final int CHUNK_SIZE = 50; // Xử lý từng chunk 50 chapter

    @Override
    public void initializeRangeJob(String jobId, Long userId) {
        // Tạo initial response
        UnlockChapterBatchResponse initialResponse = new UnlockChapterBatchResponse();
        initialResponse.setJobId(jobId);
        initialResponse.setStatus("PROCESSING");
        initialResponse.setStartTime(LocalDateTime.now());
        unlockRangeJobs.put(jobId, initialResponse);
        jobUserMap.put(jobId, userId);
        cancelFlags.put(jobId, new AtomicBoolean(false));
        log.info("Đã khởi tạo job unlock range: jobId={}, userId={}", jobId, userId);
    }

    @Override
    public void initializeFullStoryJob(String jobId, Long userId) {
        // Tạo initial response
        UnlockFullStoryResponse initialResponse = new UnlockFullStoryResponse();
        initialResponse.setJobId(jobId);
        initialResponse.setStatus("PROCESSING");
        initialResponse.setStartTime(LocalDateTime.now());
        unlockFullStoryJobs.put(jobId, initialResponse);
        jobUserMap.put(jobId, userId);
        cancelFlags.put(jobId, new AtomicBoolean(false));
        log.info("Đã khởi tạo job unlock full story: jobId={}, userId={}", jobId, userId);
    }

    @Async("chapterUnlockExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processUnlockRangeAsync(Long storyId, UnlockChapterRangeRequest request, Long userId, String jobId) {
        log.info("=== BẮT ĐẦU processUnlockRangeAsync ===");
        log.info("Thread async: {}", Thread.currentThread().getName());
        log.info("JobId: {}", jobId);

        UnlockChapterBatchResponse response = unlockRangeJobs.get(jobId);
        AtomicBoolean cancelFlag = cancelFlags.get(jobId);

        if (response == null || cancelFlag == null) {
            log.error("Không tìm thấy job hoặc cancel flag: jobId={}", jobId);
            return;
        }

        try {
            // Cập nhật trạng thái bắt đầu xử lý
            response.setStatus("PROCESSING");
            log.info("Bắt đầu xử lý unlock range: jobId={}", jobId);

            // Validation
            Story story = validateStoryExists(storyId);
            validateChapterRange(request.getFromChapterNumber(), request.getToChapterNumber());

            // Lấy danh sách chapter trong range
            List<Chapter> chapters = chapterRepository.findByStoryIdAndChapterNumberBetweenOrderByChapterNumber(
                    storyId, request.getFromChapterNumber(), request.getToChapterNumber());

            if (chapters.isEmpty()) {
                response.setStatus("FAILED");
                response.setMessage("Không tìm thấy chương nào trong khoảng đã chọn");
                log.error("Không tìm thấy chương nào: jobId={}", jobId);
                return;
            }

            // Lọc chapter bị khóa và chưa mở khóa
            List<Chapter> lockedChapters = chapters.stream()
                    .filter(chapter -> isChapterLocked(chapter.getId()))
                    .filter(chapter -> !hasUserUnlockedChapter(userId, chapter.getId()))
                    .collect(Collectors.toList());

            if (lockedChapters.isEmpty()) {
                response.setStatus("FAILED");
                response.setMessage("Tất cả chương trong khoảng đã được mở khóa hoặc không bị khóa");
                log.error("Không có chương nào cần mở khóa: jobId={}", jobId);
                return;
            }

            // Cập nhật thông tin ban đầu vào response
            response.setStoryId(story.getId());
            response.setStoryTitle(story.getTitle());
            response.setTotalChaptersUnlocked(0); // Bắt đầu từ 0
            response.setMessage("Bắt đầu mở khóa " + lockedChapters.size() + " chương từ chương " +
                    request.getFromChapterNumber() + " đến chương " + request.getToChapterNumber());

            // Tính tổng giá để hiển thị
            int totalOriginalPrice = 0;
            for (Chapter chapter : lockedChapters) {
                ChapterPayment payment = chapterPaymentRepository.findById(chapter.getId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Không tìm thấy thông tin giá chương " + chapter.getId()));
                totalOriginalPrice += payment.getPrice();
            }

            int totalFinalPrice = calculateUnlockPrice(totalOriginalPrice, lockedChapters.size(), false);

            response.setTotalOriginalPrice(totalOriginalPrice);
            response.setTotalDiscountedPrice(totalFinalPrice);
            response.setTotalSpiritStonesSpent(totalFinalPrice);
            response.setTotalDiscountPercent(
                    lockedChapters.size() > BATCH_DISCOUNT_THRESHOLD ? BATCH_DISCOUNT_PERCENT * 100 : 0.0);

            // Cập nhật lại vào map để client có thể thấy thông tin ngay
            unlockRangeJobs.put(jobId, response);

            // Xử lý từng chunk để tránh transaction quá lớn
            UnlockChapterBatchResponse finalResponse = processUnlockRangeInChunks(
                    request, userId, jobId, cancelFlag, story, lockedChapters);

            if (cancelFlag.get()) {
                response.setStatus("CANCELLED");
                response.setMessage("Mở khóa đã bị hủy");
                log.info("Job bị hủy: jobId={}", jobId);
                return;
            }

            // Lưu kết quả cuối cùng
            finalResponse.setJobId(jobId);
            finalResponse.setStatus("COMPLETED");
            finalResponse.setEndTime(LocalDateTime.now());
            unlockRangeJobs.put(jobId, finalResponse);

            log.info("Hoàn thành unlock range: jobId={}, totalChaptersUnlocked={}",
                    jobId, finalResponse.getTotalChaptersUnlocked());

        } catch (InsufficientBalanceException e) {
            log.error("Không đủ số dư unlock range: jobId={}, error={}", jobId, e.getMessage());
            response.setStatus("FAILED");
            response.setMessage("Không đủ số dư linh thạch: " + e.getMessage());
            response.setEndTime(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Lỗi unlock range: jobId={}", jobId, e);
            response.setStatus("FAILED");
            response.setMessage("Lỗi: " + e.getMessage());
            response.setEndTime(LocalDateTime.now());
        }
    }

    @Async("chapterUnlockExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processUnlockFullStoryAsync(Long storyId, UnlockFullStoryRequest request, Long userId, String jobId) {
        log.info("=== BẮT ĐẦU processUnlockFullStoryAsync (OPTIMIZED) ===");
        log.info("Thread async: {}", Thread.currentThread().getName());
        log.info("JobId: {}", jobId);

        UnlockFullStoryResponse response = unlockFullStoryJobs.get(jobId);
        AtomicBoolean cancelFlag = cancelFlags.get(jobId);

        if (response == null || cancelFlag == null) {
            log.error("Không tìm thấy job hoặc cancel flag: jobId={}", jobId);
            return;
        }

        try {
            // Cập nhật trạng thái bắt đầu xử lý
            response.setStatus("PROCESSING");
            log.info("Bắt đầu xử lý unlock full story (optimized): jobId={}", jobId);

            // Validation
            Story story = validateStoryExists(storyId);
            log.info("Đã validate story: storyId={}, title={}", storyId, story.getTitle());

            // Bước 1: Đếm tổng số chapter cần unlock (tối ưu query)
            Long totalChaptersToUnlock = chapterPaymentRepository.countChaptersToUnlockByStory(storyId, userId);
            log.info("Tổng số chapter cần unlock: {}", totalChaptersToUnlock);

            if (totalChaptersToUnlock == 0) {
                response.setStatus("FAILED");
                response.setMessage("Tất cả chương đã được mở khóa hoặc không bị khóa");
                log.info("Không có chương nào cần mở khóa: jobId={}", jobId);
                return;
            }

            // Bước 2: Tính tổng giá trước (tối ưu query)
            int totalOriginalPrice = calculateTotalPriceForUnlock(storyId, userId);
            int totalFinalPrice = calculateUnlockPrice(totalOriginalPrice, totalChaptersToUnlock.intValue(), true);

            // Bước 3: Kiểm tra số dư
            UserWallet wallet = getUserWallet(userId);
            validateSufficientBalance(wallet, totalFinalPrice);

            // Bước 4: Trừ linh thạch trước
            wallet.setSpiritStones(wallet.getSpiritStones() - totalFinalPrice);
            userWalletRepository.save(wallet);

            // Bước 5: Cập nhật thông tin ban đầu vào response
            response.setStoryId(story.getId());
            response.setStoryTitle(story.getTitle());
            response.setTotalChaptersToUnlock(totalChaptersToUnlock.intValue());
            response.setTotalChaptersUnlocked(0);
            response.setProcessedChapters(0);
            response.setTotalOriginalPrice(totalOriginalPrice);
            response.setTotalDiscountedPrice(totalFinalPrice);
            response.setTotalSpiritStonesSpent(totalFinalPrice);
            response.setTotalDiscountPercent(FULL_UNLOCK_DISCOUNT_PERCENT * 100);
            response.setProgressPercent(0.0);

            // Tính số batch
            int totalBatches = (int) Math.ceil((double) totalChaptersToUnlock / CHUNK_SIZE);
            response.setTotalBatches(totalBatches);
            response.setCurrentBatch(0);
            response.setMessage(
                    "Bắt đầu mở khóa " + totalChaptersToUnlock + " chương trong " + totalBatches + " batch...");

            // Cập nhật lại vào map để client có thể thấy thông tin ngay
            unlockFullStoryJobs.put(jobId, response);

            // Bước 6: Xử lý từng batch tối ưu
            UnlockFullStoryResponse finalResponse = processUnlockFullStoryInBatches(
                    userId, jobId, cancelFlag, story, totalChaptersToUnlock.intValue(), totalBatches);

            if (cancelFlag.get()) {
                response.setStatus("CANCELLED");
                response.setMessage("Mở khóa đã bị hủy");
                log.info("Job bị hủy: jobId={}", jobId);
                return;
            }

            // Lưu kết quả cuối cùng
            finalResponse.setJobId(jobId);
            finalResponse.setStatus("COMPLETED");
            finalResponse.setEndTime(LocalDateTime.now());
            finalResponse.setProgressPercent(100.0);
            unlockFullStoryJobs.put(jobId, finalResponse);

            log.info("Hoàn thành unlock full story (optimized): jobId={}, unlockedChapters={}",
                    jobId, finalResponse.getTotalChaptersUnlocked());

        } catch (InsufficientBalanceException e) {
            log.error("Không đủ số dư unlock full story: jobId={}, error={}", jobId, e.getMessage());
            response.setStatus("FAILED");
            response.setMessage("Không đủ số dư linh thạch: " + e.getMessage());
            response.setEndTime(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Lỗi unlock full story: jobId={}", jobId, e);
            response.setStatus("FAILED");
            response.setMessage("Lỗi: " + e.getMessage());
            response.setEndTime(LocalDateTime.now());
        }
    }

    /**
     * Xử lý unlock range theo chunks để tránh transaction quá lớn
     */
    private UnlockChapterBatchResponse processUnlockRangeInChunks(
            UnlockChapterRangeRequest request, Long userId, String jobId,
            AtomicBoolean cancelFlag, Story story, List<Chapter> lockedChapters) {

        // Tính tổng giá trước
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

        // Trừ linh thạch trước
        wallet.setSpiritStones(wallet.getSpiritStones() - totalFinalPrice);
        userWalletRepository.save(wallet);

        // Xử lý từng chunk
        int totalChapters = lockedChapters.size();
        int processedCount = 0;

        for (int i = 0; i < lockedChapters.size(); i += CHUNK_SIZE) {
            if (cancelFlag.get()) {
                break;
            }

            int chunkEnd = Math.min(i + CHUNK_SIZE, lockedChapters.size());
            List<Chapter> chunk = lockedChapters.subList(i, chunkEnd);

            try {
                // Xử lý chunk với transaction riêng
                processUnlockChunk(chunk, userId, null,
                        TransactionType.CHAPTER_UNLOCK_BATCH_SPIRIT_STONE, lockedChapters.size());

                processedCount += chunk.size();
                log.info("Hoàn thành chunk {}-{}: jobId={}, progress={}/{}",
                        i + 1, chunkEnd, jobId, processedCount, totalChapters);

                // Update progress trong map
                UnlockChapterBatchResponse progressResponse = new UnlockChapterBatchResponse();
                progressResponse.setJobId(jobId);
                progressResponse.setStatus("PROCESSING");
                progressResponse.setStartTime(unlockRangeJobs.get(jobId).getStartTime());
                progressResponse.setStoryId(story.getId());
                progressResponse.setStoryTitle(story.getTitle());
                progressResponse.setTotalChaptersUnlocked(processedCount);
                progressResponse.setTotalSpiritStonesSpent(totalFinalPrice);
                progressResponse.setTotalOriginalPrice(totalOriginalPrice);
                progressResponse.setTotalDiscountedPrice(totalFinalPrice);
                progressResponse.setTotalDiscountPercent(
                        lockedChapters.size() > BATCH_DISCOUNT_THRESHOLD ? BATCH_DISCOUNT_PERCENT * 100 : 0.0);
                progressResponse.setMessage("Đã mở khóa " + processedCount + "/" + totalChapters + " chương");
                unlockRangeJobs.put(jobId, progressResponse);

            } catch (Exception e) {
                log.error("Lỗi xử lý chunk {}-{}: jobId={}", i + 1, chunkEnd, jobId, e);
                // Tiếp tục với chunk tiếp theo
            }
        }

        // Tạo 1 transaction duy nhất cho toàn bộ quá trình unlock
        LocalDateTime unlockedAt = LocalDateTime.now();
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUser(getUserWallet(userId).getUser());
        transaction.setAmount(-totalFinalPrice);
        transaction.setType(TransactionType.CHAPTER_UNLOCK_BATCH_SPIRIT_STONE);
        transaction.setCurrency(WalletTransaction.CurrencyType.SPIRIT_STONE);
        transaction.setDescription("Mở khóa " + lockedChapters.size() + " chương từ chương " +
                request.getFromChapterNumber() + " đến chương " + request.getToChapterNumber() +
                " của truyện " + story.getTitle());
        transaction.setCreatedAt(unlockedAt);
        walletTransactionRepository.save(transaction);

        // Tạo response cuối cùng
        UnlockChapterBatchResponse finalResponse = new UnlockChapterBatchResponse();
        finalResponse.setStoryId(story.getId());
        finalResponse.setStoryTitle(story.getTitle());
        finalResponse.setTotalChaptersUnlocked(lockedChapters.size());
        finalResponse.setTotalSpiritStonesSpent(totalFinalPrice);
        finalResponse.setTotalOriginalPrice(totalOriginalPrice);
        finalResponse.setTotalDiscountedPrice(totalFinalPrice);
        finalResponse.setTotalDiscountPercent(
                lockedChapters.size() > BATCH_DISCOUNT_THRESHOLD ? BATCH_DISCOUNT_PERCENT * 100 : 0.0);
        finalResponse.setUnlockedAt(unlockedAt);

        return finalResponse;
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

    /**
     * Xử lý unlock full story theo batches tối ưu
     */
    private UnlockFullStoryResponse processUnlockFullStoryInBatches(
            Long userId, String jobId, AtomicBoolean cancelFlag, Story story,
            int totalChaptersToUnlock, int totalBatches) {

        int processedCount = 0;
        int currentBatch = 0;
        int offset = 0;

        while (processedCount < totalChaptersToUnlock && !cancelFlag.get()) {
            currentBatch++;

            // Lấy batch chapter cần unlock
            List<Object[]> chaptersBatch = chapterPaymentRepository.findChaptersToUnlockByStory(
                    story.getId(), userId, CHUNK_SIZE, offset);

            if (chaptersBatch.isEmpty()) {
                break;
            }

            try {
                // Xử lý batch với transaction riêng
                processUnlockBatch(chaptersBatch, userId, story);

                processedCount += chaptersBatch.size();
                offset += CHUNK_SIZE;

                log.info("Hoàn thành batch {}/{}: jobId={}, progress={}/{}",
                        currentBatch, totalBatches, jobId, processedCount, totalChaptersToUnlock);

                // Update progress trong map
                updateUnlockFullStoryProgress(jobId, story, totalChaptersToUnlock, processedCount,
                        currentBatch, totalBatches);

            } catch (Exception e) {
                log.error("Lỗi xử lý batch {}/{}: jobId={}", currentBatch, totalBatches, jobId, e);
                // Tiếp tục với batch tiếp theo
            }
        }

        // Tạo 1 transaction duy nhất cho toàn bộ quá trình unlock
        LocalDateTime unlockedAt = LocalDateTime.now();
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUser(getUserWallet(userId).getUser());
        transaction.setAmount(-calculateTotalPriceForUnlock(story.getId(), userId));
        transaction.setType(TransactionType.CHAPTER_UNLOCK_FULL_SPIRIT_STONE);
        transaction.setCurrency(WalletTransaction.CurrencyType.SPIRIT_STONE);
        transaction
                .setDescription("Mở khóa full truyện " + story.getTitle() + " (" + totalChaptersToUnlock + " chương)");
        transaction.setCreatedAt(unlockedAt);
        walletTransactionRepository.save(transaction);

        // Tạo response cuối cùng
        UnlockFullStoryResponse finalResponse = new UnlockFullStoryResponse();
        finalResponse.setStoryId(story.getId());
        finalResponse.setStoryTitle(story.getTitle());
        finalResponse.setTotalChaptersUnlocked(totalChaptersToUnlock);
        finalResponse.setTotalChaptersToUnlock(totalChaptersToUnlock);
        finalResponse.setProcessedChapters(processedCount);
        finalResponse.setTotalSpiritStonesSpent(calculateTotalPriceForUnlock(story.getId(), userId));
        finalResponse.setTotalOriginalPrice(calculateTotalPriceForUnlock(story.getId(), userId));
        finalResponse.setTotalDiscountedPrice(calculateTotalPriceForUnlock(story.getId(), userId));
        finalResponse.setTotalDiscountPercent(FULL_UNLOCK_DISCOUNT_PERCENT * 100);
        finalResponse.setUnlockedAt(unlockedAt);
        finalResponse.setProgressPercent(100.0);

        return finalResponse;
    }

    /**
     * Xử lý một batch chapter unlock từ Object[] (tối ưu)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void processUnlockBatch(List<Object[]> chaptersBatch, Long userId, Story story) {
        LocalDateTime unlockedAt = LocalDateTime.now();

        for (Object[] chapterData : chaptersBatch) {
            Long chapterId = (Long) chapterData[0]; // chapter_id ở index 0

            // Tạo ChapterUnlock record
            ChapterUnlockId unlockId = new ChapterUnlockId();
            unlockId.setUserId(userId);
            unlockId.setChapterId(chapterId);

            ChapterUnlock unlock = new ChapterUnlock();
            unlock.setId(unlockId);
            unlock.setUser(getUserWallet(userId).getUser());
            unlock.setChapter(chapterRepository.findById(chapterId).orElse(null));
            unlock.setUnlockedAt(unlockedAt);

            chapterUnlockRepository.save(unlock);
        }
    }

    /**
     * Cập nhật progress cho unlock full story
     */
    private void updateUnlockFullStoryProgress(String jobId, Story story, int totalChapters,
            int processedCount, int currentBatch, int totalBatches) {

        UnlockFullStoryResponse progressResponse = new UnlockFullStoryResponse();
        progressResponse.setJobId(jobId);
        progressResponse.setStatus("PROCESSING");
        progressResponse.setStartTime(unlockFullStoryJobs.get(jobId).getStartTime());
        progressResponse.setStoryId(story.getId());
        progressResponse.setStoryTitle(story.getTitle());
        progressResponse.setTotalChaptersToUnlock(totalChapters);
        progressResponse.setTotalChaptersUnlocked(processedCount);
        progressResponse.setProcessedChapters(processedCount);
        progressResponse.setCurrentBatch(currentBatch);
        progressResponse.setTotalBatches(totalBatches);
        progressResponse.setTotalSpiritStonesSpent(calculateTotalPriceForUnlock(story.getId(),
                unlockFullStoryJobs.get(jobId).getStoryId() != null ? unlockFullStoryJobs.get(jobId).getStoryId()
                        : 0L));
        progressResponse.setTotalOriginalPrice(calculateTotalPriceForUnlock(story.getId(),
                unlockFullStoryJobs.get(jobId).getStoryId() != null ? unlockFullStoryJobs.get(jobId).getStoryId()
                        : 0L));
        progressResponse.setTotalDiscountedPrice(calculateTotalPriceForUnlock(story.getId(),
                unlockFullStoryJobs.get(jobId).getStoryId() != null ? unlockFullStoryJobs.get(jobId).getStoryId()
                        : 0L));
        progressResponse.setTotalDiscountPercent(FULL_UNLOCK_DISCOUNT_PERCENT * 100);

        // Tính progress percent
        double progressPercent = (double) processedCount / totalChapters * 100;
        progressResponse.setProgressPercent(progressPercent);

        progressResponse.setMessage(String.format("Đã mở khóa %d/%d chương (Batch %d/%d) - %.1f%%",
                processedCount, totalChapters, currentBatch, totalBatches, progressPercent));
        progressResponse.setCurrentBatchInfo(String.format("Batch %d/%d", currentBatch, totalBatches));

        unlockFullStoryJobs.put(jobId, progressResponse);
    }

    /**
     * Xử lý một chunk chapter unlock - chỉ tạo ChapterUnlock records, không tạo
     * WalletTransaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void processUnlockChunk(List<Chapter> chapters, Long userId, String description,
            TransactionType transactionType, int totalChapterCount) {

        LocalDateTime unlockedAt = LocalDateTime.now();

        for (Chapter chapter : chapters) {
            // Tạo ChapterUnlock record
            ChapterUnlockId unlockId = new ChapterUnlockId();
            unlockId.setUserId(userId);
            unlockId.setChapterId(chapter.getId());

            ChapterUnlock unlock = new ChapterUnlock();
            unlock.setId(unlockId);
            unlock.setUser(getUserWallet(userId).getUser());
            unlock.setChapter(chapter);
            unlock.setUnlockedAt(unlockedAt);

            chapterUnlockRepository.save(unlock);
        }
    }

    @Override
    public Map<String, UnlockChapterBatchResponse> getUnlockRangeJobs() {
        return unlockRangeJobs;
    }

    @Override
    public Map<String, UnlockFullStoryResponse> getUnlockFullStoryJobs() {
        return unlockFullStoryJobs;
    }

    @Override
    public Map<String, Long> getJobUserMap() {
        return jobUserMap;
    }

    @Override
    public Map<String, AtomicBoolean> getCancelFlags() {
        return cancelFlags;
    }

    // Helper methods
    private boolean isChapterLocked(Long chapterId) {
        ChapterPayment payment = chapterPaymentRepository.findById(chapterId).orElse(null);
        return payment != null && Boolean.TRUE.equals(payment.getIsLocked());
    }

    private boolean hasUserUnlockedChapter(Long userId, Long chapterId) {
        return chapterUnlockRepository.existsByUserIdAndChapterId(userId, chapterId);
    }

    private int calculateUnlockPrice(int originalPrice, int chapterCount, boolean isFullUnlock) {
        double discountPercent = 0.0;

        if (isFullUnlock) {
            discountPercent = FULL_UNLOCK_DISCOUNT_PERCENT;
        } else if (chapterCount > BATCH_DISCOUNT_THRESHOLD) {
            discountPercent = BATCH_DISCOUNT_PERCENT;
        }

        return (int) Math.round(originalPrice * (1 - discountPercent));
    }

    private Story validateStoryExists(Long storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + storyId));
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

    private List<Chapter> getAllChaptersByStoryId(Long storyId) {
        return chapterRepository.findByStoryIdAndChapterNumberBetweenOrderByChapterNumber(storyId, 1,
                Integer.MAX_VALUE);
    }
}
