package com.meobeo.truyen.domain.request.chapter;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateChapterRequest {

    @NotNull(message = "Story ID không được để trống")
    private Long storyId;

    @NotNull(message = "Số chapter không được để trống")
    @Min(value = 1, message = "Số chapter phải lớn hơn 0")
    private Integer chapterNumber;

    @NotBlank(message = "Slug không được để trống")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug chỉ được chứa chữ thường, số và dấu gạch ngang")
    @Size(min = 1, max = 200, message = "Slug phải từ 1-200 ký tự")
    private String slug;

    @Size(max = 255, message = "Tiêu đề không được quá 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(min = 10, message = "Nội dung phải có ít nhất 10 ký tự")
    private String content;
}
