package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.story.TxtImportRequest;
import com.meobeo.truyen.domain.response.story.TxtImportResponse;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.TxtImportService;
import com.meobeo.truyen.service.interfaces.AsyncTxtImportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class TxtImportServiceImpl implements TxtImportService {

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final AsyncTxtImportService asyncTxtImportService;

    @Override
    public String startTxtImport(MultipartFile txtFile, TxtImportRequest request, Long userId) {
        log.info("=== BẮT ĐẦU startTxtImport ===");
        log.info("Thread hiện tại: {}", Thread.currentThread().getName());

        // Validation
        if (txtFile == null || txtFile.isEmpty()) {
            throw new BadRequestException("File TXT không được để trống");
        }

        if (!isTxtFile(txtFile)) {
            throw new BadRequestException("File phải có định dạng TXT");
        }

        log.info("Validation file xong, bắt đầu query database...");

        // Kiểm tra user tồn tại
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        log.info("Query user xong, bắt đầu query story...");

        // Kiểm tra story tồn tại và quyền truy cập
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + request.getStoryId()));

        log.info("Query story xong, bắt đầu kiểm tra quyền...");

        // Kiểm tra quyền: chỉ author hoặc admin mới được import
        if (!story.getAuthor().getId().equals(userId)) {
            User user = userRepository.findById(userId).orElseThrow();
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(role -> "ADMIN".equals(role.getName()));
            if (!isAdmin) {
                throw new BadRequestException("Bạn không có quyền import chapter cho truyện này");
            }
        }

        log.info("Kiểm tra quyền xong, tạo jobId...");

        // Tạo job ID
        String jobId = UUID.randomUUID().toString();

        log.info("JobId tạo xong: {}", jobId);

        // Tạo response ban đầu
        TxtImportResponse response = new TxtImportResponse();
        response.setJobId(jobId);
        response.setStatus("PENDING");
        response.setStartTime(LocalDateTime.now());
        response.setStoryId(story.getId());
        response.setStorySlug(story.getSlug());
        response.setStoryTitle(story.getTitle());
        response.setMessage("Đã nhận yêu cầu import. Đang chuẩn bị xử lý...");

        // Sử dụng AsyncTxtImportService để xử lý bất đồng bộ
        asyncTxtImportService.getImportJobs().put(jobId, response);
        asyncTxtImportService.getJobUserMap().put(jobId, userId);
        asyncTxtImportService.getCancelFlags().put(jobId, new AtomicBoolean(false));

        log.info("Bắt đầu gọi async task...");
        log.info("Thread trước khi gọi async: {}", Thread.currentThread().getName());

        // Bắt đầu xử lý async - KHÔNG chờ kết quả
        asyncTxtImportService.processTxtFileAsync(txtFile, request, userId, jobId);

        log.info("Async task đã được gọi, trả về jobId: {}", jobId);
        log.info("Thread sau khi gọi async: {}", Thread.currentThread().getName());

        return jobId;
    }

    public boolean isTxtFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }
        return originalFilename.toLowerCase().endsWith(".txt");
    }

    @Override
    public Optional<TxtImportResponse> getImportStatus(String jobId) {
        return Optional.ofNullable(asyncTxtImportService.getImportJobs().get(jobId));
    }

    @Override
    public List<TxtImportResponse> getUserJobs(Long userId) {
        List<TxtImportResponse> userJobs = new ArrayList<>();

        for (Map.Entry<String, Long> entry : asyncTxtImportService.getJobUserMap().entrySet()) {
            if (entry.getValue().equals(userId)) {
                TxtImportResponse job = asyncTxtImportService.getImportJobs().get(entry.getKey());
                if (job != null) {
                    userJobs.add(job);
                }
            }
        }

        // Sắp xếp theo thời gian tạo mới nhất
        userJobs.sort((a, b) -> b.getStartTime().compareTo(a.getStartTime()));

        return userJobs;
    }

    @Override
    public boolean cancelImport(String jobId, Long userId) {
        // Kiểm tra quyền: chỉ user tạo job hoặc admin mới được hủy
        Long jobUserId = asyncTxtImportService.getJobUserMap().get(jobId);
        if (jobUserId == null || !jobUserId.equals(userId)) {
            // Kiểm tra xem user có phải admin không
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getRoles().stream()
                    .noneMatch(role -> "ADMIN".equals(role.getName()))) {
                return false;
            }
        }

        AtomicBoolean cancelFlag = asyncTxtImportService.getCancelFlags().get(jobId);
        if (cancelFlag != null) {
            cancelFlag.set(true);
            TxtImportResponse response = asyncTxtImportService.getImportJobs().get(jobId);
            if (response != null) {
                response.setStatus("CANCELLED");
                response.setMessage("Import đã bị hủy bởi người dùng");
            }
            return true;
        }
        return false;
    }

    @Override
    public void cleanupCompletedJob(String jobId) {
        asyncTxtImportService.getImportJobs().remove(jobId);
        asyncTxtImportService.getCancelFlags().remove(jobId);
        asyncTxtImportService.getJobUserMap().remove(jobId);
    }

    /**
     * Cleanup tất cả job đã hoàn thành (COMPLETED, FAILED, CANCELLED) cũ hơn 24 giờ
     * Được gọi bởi scheduled task
     */
    public void cleanupOldJobs() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<String> jobsToRemove = new ArrayList<>();

        for (Map.Entry<String, TxtImportResponse> entry : asyncTxtImportService.getImportJobs().entrySet()) {
            TxtImportResponse job = entry.getValue();
            String status = job.getStatus();

            // Chỉ cleanup job đã hoàn thành và cũ hơn 24 giờ
            if (("COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status))
                    && job.getStartTime().isBefore(cutoffTime)) {
                jobsToRemove.add(entry.getKey());
            }
        }

        for (String jobId : jobsToRemove) {
            cleanupCompletedJob(jobId);
            log.info("Đã cleanup job cũ: jobId={}", jobId);
        }

        if (!jobsToRemove.isEmpty()) {
            log.info("Đã cleanup {} job cũ", jobsToRemove.size());
        }
    }

    /**
     * Scheduled task để cleanup job cũ mỗi giờ
     */
    @Scheduled(fixedRate = 3600000) // 1 giờ = 3600000ms
    public void scheduledCleanup() {
        try {
            cleanupOldJobs();
        } catch (Exception e) {
            log.error("Lỗi trong scheduled cleanup: {}", e.getMessage(), e);
        }
    }

}
