package com.meobeo.truyen.domain.response.story;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TxtImportResponse {

    /**
     * Job ID để track progress
     */
    private String jobId;

    /**
     * Trạng thái job: PROCESSING, COMPLETED, FAILED, CANCELLED
     */
    private String status;

    /**
     * ID truyện được tạo/cập nhật
     */
    private Long storyId;

    /**
     * Slug truyện
     */
    private String storySlug;

    /**
     * Tên truyện
     */
    private String storyTitle;

    /**
     * Thời gian bắt đầu
     */
    private LocalDateTime startTime;

    /**
     * Thời gian kết thúc
     */
    private LocalDateTime endTime;

    /**
     * Tổng số chapter cần import
     */
    private Integer totalChapters;

    /**
     * Số chapter đã import thành công
     */
    private Integer successCount = 0;

    /**
     * Số chapter import thất bại
     */
    private Integer failureCount = 0;

    /**
     * Số chapter đã xử lý
     */
    private Integer processedCount = 0;

    /**
     * Phần trăm hoàn thành (0-100)
     */
    private Double progressPercentage = 0.0;

    /**
     * Thời gian dự kiến hoàn thành (seconds)
     */
    private Long estimatedTimeRemaining;

    /**
     * Batch hiện tại đang xử lý
     */
    private Integer currentBatch;

    /**
     * Tổng số batch
     */
    private Integer totalBatches;

    /**
     * Danh sách lỗi
     */
    private List<String> errors = new ArrayList<>();

    /**
     * Thông tin chi tiết
     */
    private String message;

    /**
     * Cập nhật progress
     */
    public void updateProgress() {
        if (totalChapters != null && totalChapters > 0) {
            this.progressPercentage = (double) processedCount / totalChapters * 100.0;
        }
    }

    /**
     * Thêm lỗi
     */
    public void addError(String error) {
        this.errors.add(error);
    }
}
