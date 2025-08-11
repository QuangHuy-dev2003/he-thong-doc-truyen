package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.story.FormatFileRequest;
import com.meobeo.truyen.domain.response.story.FormatFileResponse;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.FormatFileService;
import com.meobeo.truyen.service.interfaces.AsyncFormatFileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormatFileServiceImpl implements FormatFileService {

    private final UserRepository userRepository;
    private final AsyncFormatFileService asyncFormatFileService;

    @Override
    public String startFormatFile(MultipartFile txtFile, FormatFileRequest request, Long userId) {
        log.info("=== BẮT ĐẦU startFormatFile ===");
        log.info("Thread hiện tại: {}", Thread.currentThread().getName());

        // Validation
        if (txtFile == null || txtFile.isEmpty()) {
            throw new BadRequestException("File TXT không được để trống");
        }

        if (!isTxtFile(txtFile)) {
            throw new BadRequestException("File phải có định dạng TXT");
        }

        // Kiểm tra kích thước file (giới hạn 100MB)
        if (txtFile.getSize() > 100 * 1024 * 1024) {
            throw new BadRequestException("File quá lớn. Kích thước tối đa là 100MB");
        }

        log.info("Validation file xong, bắt đầu query database...");

        // Kiểm tra user tồn tại
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        log.info("Query user xong, tạo jobId...");

        // Tạo job ID
        String jobId = UUID.randomUUID().toString();

        log.info("JobId tạo xong: {}", jobId);

        // Tạo response ban đầu
        FormatFileResponse response = new FormatFileResponse();
        response.setJobId(jobId);
        response.setStatus("PENDING");
        response.setStartTime(LocalDateTime.now());
        response.setOriginalFileName(txtFile.getOriginalFilename());
        response.setMessage("Đã nhận yêu cầu format. Đang chuẩn bị xử lý...");

        // Sử dụng AsyncFormatFileService để xử lý bất đồng bộ
        asyncFormatFileService.getFormatJobs().put(jobId, response);
        asyncFormatFileService.getJobUserMap().put(jobId, userId);
        asyncFormatFileService.getCancelFlags().put(jobId, new AtomicBoolean(false));

        log.info("Bắt đầu gọi async task...");
        log.info("Thread trước khi gọi async: {}", Thread.currentThread().getName());

        // Bắt đầu xử lý async - KHÔNG chờ kết quả
        asyncFormatFileService.processFormatFileAsync(txtFile, request, userId, jobId);

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
    public Optional<FormatFileResponse> getFormatStatus(String jobId) {
        return Optional.ofNullable(asyncFormatFileService.getFormatJobs().get(jobId));
    }

    @Override
    public List<FormatFileResponse> getUserFormatJobs(Long userId) {
        List<FormatFileResponse> userJobs = new ArrayList<>();

        for (Map.Entry<String, Long> entry : asyncFormatFileService.getJobUserMap().entrySet()) {
            if (entry.getValue().equals(userId)) {
                FormatFileResponse job = asyncFormatFileService.getFormatJobs().get(entry.getKey());
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
    public boolean cancelFormat(String jobId, Long userId) {
        // Kiểm tra quyền: chỉ user tạo job hoặc admin mới được hủy
        Long jobUserId = asyncFormatFileService.getJobUserMap().get(jobId);
        if (jobUserId == null || !jobUserId.equals(userId)) {
            // Kiểm tra xem user có phải admin không
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getRoles().stream()
                    .noneMatch(role -> "ADMIN".equals(role.getName()))) {
                return false;
            }
        }

        AtomicBoolean cancelFlag = asyncFormatFileService.getCancelFlags().get(jobId);
        if (cancelFlag != null) {
            cancelFlag.set(true);
            FormatFileResponse response = asyncFormatFileService.getFormatJobs().get(jobId);
            if (response != null) {
                response.setStatus("CANCELLED");
                response.setMessage("Format đã bị hủy bởi người dùng");
            }
            return true;
        }
        return false;
    }

    @Override
    public void cleanupCompletedFormatJob(String jobId) {
        // Xóa file đã format nếu có
        FormatFileResponse response = asyncFormatFileService.getFormatJobs().get(jobId);
        if (response != null && response.getFormattedFileName() != null) {
            try {
                Path filePath = Paths.get("formatted_files", response.getFormattedFileName());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Đã xóa file đã format: {}", response.getFormattedFileName());
                }
            } catch (IOException e) {
                log.warn("Không thể xóa file đã format: {}", e.getMessage());
            }
        }

        asyncFormatFileService.getFormatJobs().remove(jobId);
        asyncFormatFileService.getCancelFlags().remove(jobId);
        asyncFormatFileService.getJobUserMap().remove(jobId);
    }

    @Override
    public byte[] downloadFormattedFile(String jobId, Long userId) {
        // Kiểm tra quyền
        Long jobUserId = asyncFormatFileService.getJobUserMap().get(jobId);
        if (jobUserId == null || !jobUserId.equals(userId)) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getRoles().stream()
                    .noneMatch(role -> "ADMIN".equals(role.getName()))) {
                throw new BadRequestException("Bạn không có quyền download file này");
            }
        }

        // Kiểm tra job đã hoàn thành chưa
        FormatFileResponse response = asyncFormatFileService.getFormatJobs().get(jobId);
        if (response == null) {
            throw new ResourceNotFoundException("Không tìm thấy job format với ID: " + jobId);
        }

        if (!"COMPLETED".equals(response.getStatus())) {
            throw new BadRequestException("Job chưa hoàn thành. Trạng thái hiện tại: " + response.getStatus());
        }

        if (response.getFormattedFileName() == null) {
            throw new BadRequestException("Không tìm thấy file đã format");
        }

        // Đọc file
        try {
            Path filePath = Paths.get("formatted_files", response.getFormattedFileName());
            if (!Files.exists(filePath)) {
                throw new ResourceNotFoundException("File đã format không tồn tại");
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Lỗi đọc file đã format: jobId={}, error={}", jobId, e.getMessage());
            throw new BadRequestException("Lỗi đọc file: " + e.getMessage());
        }
    }

    /**
     * Cleanup tất cả job đã hoàn thành (COMPLETED, FAILED, CANCELLED) cũ hơn 24 giờ
     * Được gọi bởi scheduled task
     */
    public void cleanupOldFormatJobs() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<String> jobsToRemove = new ArrayList<>();

        for (Map.Entry<String, FormatFileResponse> entry : asyncFormatFileService.getFormatJobs().entrySet()) {
            FormatFileResponse job = entry.getValue();
            String status = job.getStatus();

            // Chỉ cleanup job đã hoàn thành và cũ hơn 24 giờ
            if (("COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status))
                    && job.getStartTime().isBefore(cutoffTime)) {
                jobsToRemove.add(entry.getKey());
            }
        }

        for (String jobId : jobsToRemove) {
            cleanupCompletedFormatJob(jobId);
            log.info("Đã cleanup job format cũ: jobId={}", jobId);
        }

        if (!jobsToRemove.isEmpty()) {
            log.info("Đã cleanup {} job format cũ", jobsToRemove.size());
        }
    }

    /**
     * Scheduled task để cleanup job cũ mỗi giờ
     */
    @Scheduled(fixedRate = 3600000) // 1 giờ = 3600000ms
    public void scheduledCleanup() {
        try {
            cleanupOldFormatJobs();
        } catch (Exception e) {
            log.error("Lỗi trong scheduled cleanup format jobs: {}", e.getMessage(), e);
        }
    }
}
