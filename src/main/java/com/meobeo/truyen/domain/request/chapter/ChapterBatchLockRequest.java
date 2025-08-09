package com.meobeo.truyen.domain.request.chapter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChapterBatchLockRequest {

    @NotNull(message = "Story ID không được để trống")
    private Long storyId;

    @NotNull(message = "Giá tiền không được để trống")
    @Min(value = 0, message = "Giá tiền phải >= 0")
    private Integer price;

    private Boolean isVipOnly = false;

    // Để khóa 1 chapter cụ thể
    private Long chapterId;

    // Để khóa theo range chapter number (từ chapterStart đến chapterEnd)
    @Min(value = 1, message = "Chapter bắt đầu phải >= 1")
    private Integer chapterStart;

    @Min(value = 1, message = "Chapter kết thúc phải >= 1")
    private Integer chapterEnd;

    /**
     * Validation: Phải có ít nhất 1 trong 2 cách:
     * - chapterId (khóa 1 chapter cụ thể)
     * - chapterStart + chapterEnd (khóa range)
     */
    public boolean isValid() {
        // Khóa 1 chapter cụ thể
        if (chapterId != null) {
            return chapterStart == null && chapterEnd == null;
        }

        // Khóa range chapter
        if (chapterStart != null && chapterEnd != null) {
            return chapterId == null && chapterStart <= chapterEnd;
        }

        return false;
    }

    /**
     * Kiểm tra có phải khóa single chapter không
     */
    public boolean isSingleChapter() {
        return chapterId != null;
    }

    /**
     * Kiểm tra có phải khóa range chapter không
     */
    public boolean isRangeChapter() {
        return chapterStart != null && chapterEnd != null;
    }
}
