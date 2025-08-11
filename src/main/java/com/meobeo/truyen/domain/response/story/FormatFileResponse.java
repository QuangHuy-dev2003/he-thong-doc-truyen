package com.meobeo.truyen.domain.response.story;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO cho API format file truyện
 */
@Data
public class FormatFileResponse {

    /**
     * Job ID để track progress
     */
    private String jobId;

    /**
     * Trạng thái job: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
     */
    private String status;

    /**
     * Thông báo hiện tại
     */
    private String message;

    /**
     * Thời gian bắt đầu
     */
    private LocalDateTime startTime;

    /**
     * Thời gian kết thúc
     */
    private LocalDateTime endTime;

    /**
     * Tên file gốc
     */
    private String originalFileName;

    /**
     * Tên file đã format
     */
    private String formattedFileName;

    /**
     * Kích thước file gốc (bytes)
     */
    private Long originalFileSize;

    /**
     * Kích thước file đã format (bytes)
     */
    private Long formattedFileSize;

    /**
     * Số dòng gốc
     */
    private Integer originalLineCount;

    /**
     * Số dòng sau format
     */
    private Integer formattedLineCount;

    /**
     * Số dòng đã xử lý
     */
    private Integer processedLines;

    /**
     * Tổng số dòng cần xử lý
     */
    private Integer totalLines;

    /**
     * Tiến độ xử lý (0-100)
     */
    private Integer progress;

    /**
     * URL download file đã format (khi hoàn thành)
     */
    private String downloadUrl;

    /**
     * Danh sách lỗi nếu có
     */
    private List<String> errors = new ArrayList<>();

    /**
     * Thống kê format
     */
    private FormatStats stats = new FormatStats();

    /**
     * Cập nhật tiến độ
     */
    public void updateProgress() {
        if (totalLines > 0) {
            this.progress = Math.min(100, (processedLines * 100) / totalLines);
        }
    }

    /**
     * Thêm lỗi
     */
    public void addError(String error) {
        this.errors.add(error);
    }

    /**
     * Thống kê format
     */
    @Data
    public static class FormatStats {
        /**
         * Số dòng watermark đã xóa
         */
        private Integer watermarkLinesRemoved = 0;

        /**
         * Số dòng ký tự đặc biệt đã xóa
         */
        private Integer specialCharLinesRemoved = 0;

        /**
         * Số dòng trống đã gộp
         */
        private Integer emptyLinesMerged = 0;

        /**
         * Số dòng đã format dấu câu
         */
        private Integer punctuationLinesFormatted = 0;

        /**
         * Số dòng chỉ chứa ký tự đặc biệt đã xóa
         */
        private Integer specialOnlyLinesRemoved = 0;
    }
}
