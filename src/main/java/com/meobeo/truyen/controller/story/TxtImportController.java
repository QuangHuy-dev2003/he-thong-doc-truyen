package com.meobeo.truyen.controller.story;

import com.meobeo.truyen.domain.request.story.TxtImportRequest;
import com.meobeo.truyen.domain.response.story.TxtImportResponse;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.service.interfaces.TxtImportService;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TxtImportController {

    private final TxtImportService txtImportService;
    private final SecurityUtils securityUtils;

    /**
     * API upload và import file TXT
     * Chỉ cho phép UPLOADER và ADMIN
     */
    @PostMapping(value = "/stories/txt/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> importTxtFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("storyId") Long storyId,
            @RequestParam(value = "startFromChapter", defaultValue = "1") Integer startFromChapter,
            @RequestParam(value = "endAtChapter", required = false) Integer endAtChapter,
            @RequestParam(value = "batchSize", defaultValue = "10") Integer batchSize,
            @RequestParam(value = "overwriteExisting", defaultValue = "false") Boolean overwriteExisting,
            @RequestParam(value = "chapterSlugPrefix", required = false) String chapterSlugPrefix) {

        try {
            // Validation file
            if (file == null || file.isEmpty()) {
                throw new BadRequestException("File TXT không được để trống");
            }

            // Validation storyId
            if (storyId == null || storyId <= 0) {
                throw new BadRequestException("Story ID không hợp lệ");
            }

            // Validation batch size
            if (batchSize < 1 || batchSize > 50) {
                throw new BadRequestException("Batch size phải từ 1 đến 50");
            }

            // Validation chapter range
            if (startFromChapter < 1) {
                throw new BadRequestException("Chương bắt đầu phải >= 1");
            }

            if (endAtChapter != null && endAtChapter < startFromChapter) {
                throw new BadRequestException("Chương kết thúc phải >= chương bắt đầu");
            }

            // Tạo request object
            TxtImportRequest request = new TxtImportRequest();
            request.setStoryId(storyId);
            request.setStartFromChapter(startFromChapter);
            request.setEndAtChapter(endAtChapter);
            request.setBatchSize(batchSize);
            request.setOverwriteExisting(overwriteExisting);
            request.setChapterSlugPrefix(chapterSlugPrefix);

            // Lấy user ID
            Long userId = securityUtils.getCurrentUserIdOrThrow();

            // Bắt đầu import
            String jobId = txtImportService.startTxtImport(file, request, userId);

            log.info("Bắt đầu import TXT: file={}, jobId={}, userId={}",
                    file.getOriginalFilename(), jobId, userId);

            return ResponseEntity
                    .ok(ApiResponse.success("Đã bắt đầu import TXT. Sử dụng job ID để theo dõi tiến độ.", jobId));

        } catch (Exception e) {
            log.error("Lỗi import TXT: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi import TXT: " + e.getMessage()));
        }
    }

    /**
     * API kiểm tra trạng thái import
     */
    @GetMapping("/txt/status/{jobId}")
    @PreAuthorize("hasRole('USER') or hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TxtImportResponse>> getImportStatus(@PathVariable String jobId) {

        try {
            TxtImportResponse status = txtImportService.getImportStatus(jobId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy job import với ID: " + jobId));

            return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái import thành công", status));

        } catch (Exception e) {
            log.error("Lỗi lấy trạng thái import: jobId={}, error={}", jobId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy trạng thái import: " + e.getMessage()));
        }
    }

    /**
     * API hủy import đang chạy
     */
    @PostMapping("/txt/cancel/{jobId}")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> cancelImport(@PathVariable String jobId) {

        try {
            Long userId = securityUtils.getCurrentUserIdOrThrow();
            boolean cancelled = txtImportService.cancelImport(jobId, userId);

            if (cancelled) {
                log.info("Đã hủy import: jobId={}, userId={}", jobId, userId);
                return ResponseEntity.ok(ApiResponse.success("Đã hủy import thành công", cancelled));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Không thể hủy import. Job có thể đã hoàn thành hoặc không tồn tại."));
            }

        } catch (Exception e) {
            log.error("Lỗi hủy import: jobId={}, error={}", jobId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi hủy import: " + e.getMessage()));
        }
    }

    /**
     * API xóa job đã hoàn thành (cleanup)
     */
    @DeleteMapping("/txt/cleanup/{jobId}")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> cleanupJob(@PathVariable String jobId) {

        try {
            txtImportService.cleanupCompletedJob(jobId);

            log.info("Đã cleanup job: jobId={}", jobId);
            return ResponseEntity.ok(ApiResponse.success("Đã cleanup job thành công", "OK"));

        } catch (Exception e) {
            log.error("Lỗi cleanup job: jobId={}, error={}", jobId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi cleanup job: " + e.getMessage()));
        }
    }

    /**
     * API lấy thông tin hỗ trợ import TXT
     */
    @GetMapping("/txt/info")
    public ResponseEntity<ApiResponse<TxtImportInfo>> getImportInfo() {

        TxtImportInfo info = new TxtImportInfo();
        info.maxFileSize = "50MB";
        info.supportedFormats = new String[] { "TXT" };
        info.maxBatchSize = 50;
        info.defaultBatchSize = 10;
        info.estimatedTimePerChapter = "1-3 giây";
        info.notes = new String[] {
                "Truyện phải được tạo trước khi import chapter từ TXT",
                "Chỉ ADMIN và UPLOADER có quyền import",
                "Hệ thống tự động phân tích và phát hiện chương theo định dạng 'Chương X: Title'",
                "Xử lý file theo stream - chỉ đọc và xử lý chương trong range yêu cầu",
                "Tối ưu memory cho file lớn (hỗ trợ file 2000+ chương)",
                "Phát hiện ranh giới chapter - khi gặp chương mới thì chương trước kết thúc",
                "Batch size càng lớn thì import càng nhanh nhưng tốn RAM hơn",
                "Có thể theo dõi tiến độ real-time qua API status",
                "Hỗ trợ overwrite chapter đã tồn tại",
                "Tự động tạo slug từ title của chương"
        };

        return ResponseEntity.ok(ApiResponse.success("Thông tin import TXT", info));
    }

    // Inner class for import info
    public static class TxtImportInfo {
        public String maxFileSize;
        public String[] supportedFormats;
        public Integer maxBatchSize;
        public Integer defaultBatchSize;
        public String estimatedTimePerChapter;
        public String[] notes;
    }
}
