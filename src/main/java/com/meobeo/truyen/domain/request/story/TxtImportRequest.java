package com.meobeo.truyen.domain.request.story;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class TxtImportRequest {

    /**
     * ID của story đã tồn tại để import chapter vào (bắt buộc)
     */
    @NotNull(message = "Story ID không được để trống")
    private Long storyId;

    /**
     * Bắt đầu import từ chương số mấy (mặc định = 1)
     */
    @Min(value = 1, message = "Chương bắt đầu phải >= 1")
    private Integer startFromChapter = 1;

    /**
     * Kết thúc import ở chương số mấy (optional - nếu null sẽ import hết)
     */
    private Integer endAtChapter;

    /**
     * Số lượng chapter mỗi batch (mặc định = 10, tối đa 50)
     */
    @Min(value = 1, message = "Batch size phải >= 1")
    @Max(value = 50, message = "Batch size không được > 50")
    private Integer batchSize = 10;

    /**
     * Có overwrite chapter đã tồn tại không (mặc định = false)
     */
    private Boolean overwriteExisting = false;

    /**
     * Prefix cho slug của chapter (optional - nếu không có sẽ dùng story slug)
     * Ví dụ: "chuong" -> "chuong-1-title", "chuong-2-title"
     */
    private String chapterSlugPrefix;
}
