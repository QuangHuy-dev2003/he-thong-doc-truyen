package com.meobeo.truyen.domain.request.chapter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnlockChapterRangeRequest {

    @NotNull(message = "Story ID không được để trống")
    private Long storyId;

    @NotNull(message = "Số chương bắt đầu không được để trống")
    @Min(value = 1, message = "Số chương bắt đầu phải lớn hơn 0")
    private Integer fromChapterNumber;

    @NotNull(message = "Số chương kết thúc không được để trống")
    @Min(value = 1, message = "Số chương kết thúc phải lớn hơn 0")
    @Max(value = 10000, message = "Số chương kết thúc không được vượt quá 10000")
    private Integer toChapterNumber;

    private String description;
}
