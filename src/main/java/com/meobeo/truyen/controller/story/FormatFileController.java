package com.meobeo.truyen.controller.story;

import com.meobeo.truyen.domain.request.story.FormatFileRequest;
import com.meobeo.truyen.domain.response.story.FormatFileResponse;
import com.meobeo.truyen.service.interfaces.FormatFileService;
import com.meobeo.truyen.utils.ApiResponse;
import com.meobeo.truyen.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class FormatFileController {

    private final FormatFileService formatFileService;
    private final SecurityUtils securityUtils;

    /**
     * API upload và format file TXT bất đồng bộ
     * Chỉ cho phép UPLOADER và ADMIN
     * Trả về jobId ngay lập tức để theo dõi tiến độ
     */
    @PostMapping(value = "/format/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> formatFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "removeWatermark", defaultValue = "true") Boolean removeWatermark,
            @RequestParam(value = "removeSpecialChars", defaultValue = "true") Boolean removeSpecialChars,
            @RequestParam(value = "mergeEmptyLines", defaultValue = "true") Boolean mergeEmptyLines,
            @RequestParam(value = "formatPunctuation", defaultValue = "true") Boolean formatPunctuation) {

        try {
            // Validation file
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File TXT không được để trống");
            }

            // Tạo request object
            FormatFileRequest request = new FormatFileRequest();
            request.setOriginalFileName(file.getOriginalFilename());
            request.setRemoveWatermark(removeWatermark);
            request.setRemoveSpecialChars(removeSpecialChars);
            request.setMergeEmptyLines(mergeEmptyLines);
            request.setFormatPunctuation(formatPunctuation);

            // Lấy user ID
            Long userId = securityUtils.getCurrentUserIdOrThrow();

            // Bắt đầu format bất đồng bộ - trả về jobId ngay lập tức
            String jobId = formatFileService.startFormatFile(file, request, userId);

            log.info("Đã khởi tạo job format file: file={}, jobId={}, userId={}",
                    file.getOriginalFilename(), jobId, userId);

            return ResponseEntity
                    .ok(ApiResponse.success(
                            "Đã bắt đầu format file bất đồng bộ. Sử dụng job ID để theo dõi tiến độ.",
                            jobId));

        } catch (Exception e) {
            log.error("Lỗi khởi tạo format file: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khởi tạo format file: " + e.getMessage()));
        }
    }

    /**
     * API kiểm tra trạng thái format
     */
    @GetMapping("/format/status/{jobId}")
    @PreAuthorize("hasRole('USER') or hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FormatFileResponse>> getFormatStatus(@PathVariable String jobId) {

        try {
            FormatFileResponse status = formatFileService.getFormatStatus(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy job format với ID: " + jobId));

            return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái format thành công", status));

        } catch (Exception e) {
            log.error("Lỗi lấy trạng thái format: jobId={}, error={}", jobId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy trạng thái format: " + e.getMessage()));
        }
    }

    /**
     * API hủy format đang chạy
     */
    @PostMapping("/format/cancel/{jobId}")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> cancelFormat(@PathVariable String jobId) {

        try {
            Long userId = securityUtils.getCurrentUserIdOrThrow();
            boolean cancelled = formatFileService.cancelFormat(jobId, userId);

            if (cancelled) {
                log.info("Đã hủy format: jobId={}, userId={}", jobId, userId);
                return ResponseEntity.ok(ApiResponse.success("Đã hủy format thành công", cancelled));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Không thể hủy format. Job có thể đã hoàn thành hoặc không tồn tại."));
            }

        } catch (Exception e) {
            log.error("Lỗi hủy format: jobId={}, error={}", jobId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi hủy format: " + e.getMessage()));
        }
    }

    /**
     * API lấy danh sách job format của user hiện tại
     */
    @GetMapping("/format/jobs")
    @PreAuthorize("hasRole('USER') or hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FormatFileResponse>>> getUserFormatJobs() {

        try {
            Long userId = securityUtils.getCurrentUserIdOrThrow();
            List<FormatFileResponse> userJobs = formatFileService.getUserFormatJobs(userId);

            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách job format thành công", userJobs));

        } catch (Exception e) {
            log.error("Lỗi lấy danh sách job format: userId={}, error={}",
                    securityUtils.getCurrentUserIdOrThrow(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy danh sách job format: " + e.getMessage()));
        }
    }

    /**
     * API download file đã format
     */
    @GetMapping("/format/download/{jobId}")
    @PreAuthorize("hasRole('USER') or hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadFormattedFile(@PathVariable String jobId) {

        try {
            Long userId = securityUtils.getCurrentUserIdOrThrow();
            byte[] fileContent = formatFileService.downloadFormattedFile(jobId, userId);

            // Lấy tên file từ job
            FormatFileResponse response = formatFileService.getFormatStatus(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy job format"));

            String fileName = response.getFormattedFileName() != null ? response.getFormattedFileName()
                    : "formatted_file.txt";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", fileName);

            log.info("Download file đã format: jobId={}, fileName={}, size={} bytes",
                    jobId, fileName, fileContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);

        } catch (Exception e) {
            log.error("Lỗi download file đã format: jobId={}, error={}", jobId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * API xóa job đã hoàn thành (cleanup)
     */
    @DeleteMapping("/format/cleanup/{jobId}")
    @PreAuthorize("hasRole('UPLOADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> cleanupFormatJob(@PathVariable String jobId) {

        try {
            formatFileService.cleanupCompletedFormatJob(jobId);

            log.info("Đã cleanup job format: jobId={}", jobId);
            return ResponseEntity.ok(ApiResponse.success("Đã cleanup job format thành công", "OK"));

        } catch (Exception e) {
            log.error("Lỗi cleanup job format: jobId={}, error={}", jobId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi cleanup job format: " + e.getMessage()));
        }
    }

    /**
     * API lấy thông tin hỗ trợ format file
     */
    @GetMapping("/format/info")
    public ResponseEntity<ApiResponse<FormatFileInfo>> getFormatInfo() {

        FormatFileInfo info = new FormatFileInfo();
        info.maxFileSize = "100MB";
        info.supportedFormats = new String[] { "TXT" };
        info.estimatedTimePerLine = "0.1-0.5 giây";
        info.features = new String[] {
                "Tự động loại bỏ khoảng trắng dư thừa ở đầu/cuối dòng",
                "Xoá toàn bộ các ký tự gạch đầu dòng (-, –, —, •, *) khi chúng đứng ở đầu dòng",
                "Loại bỏ tất cả watermark hoặc chữ ký nhóm dịch",
                "Gộp nhiều khoảng trắng liên tiếp trong 1 dòng thành 1 khoảng trắng duy nhất",
                "Xoá khoảng trắng thừa trước dấu câu và đảm bảo có đúng 1 khoảng trắng sau dấu câu",
                "Loại bỏ các dòng chỉ chứa ký tự đặc biệt như *** --- ___",
                "Gộp nhiều dòng trống liên tiếp thành tối đa 1 dòng trống",
                "Xử lý encoding UTF-8 chuẩn, loại bỏ ký tự vô hình",
                "Hệ thống xử lý bất đồng bộ - trả về jobId ngay lập tức",
                "Sử dụng API /format/status/{jobId} để theo dõi tiến độ real-time",
                "Sử dụng API /format/jobs để xem danh sách job của bạn",
                "Có thể theo dõi tiến độ real-time qua API status",
                "Có thể hủy job đang chạy qua API /format/cancel/{jobId}",
                "Job hoàn thành sẽ tự động cleanup sau 24 giờ",
                "File đã format được lưu với encoding UTF-8"
        };

        return ResponseEntity.ok(ApiResponse.success("Thông tin format file", info));
    }

    // Inner class for format info
    public static class FormatFileInfo {
        public String maxFileSize;
        public String[] supportedFormats;
        public String estimatedTimePerLine;
        public String[] features;
    }
}
