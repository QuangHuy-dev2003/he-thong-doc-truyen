package com.meobeo.truyen.domain.request.chapter;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateChapterRequest {

    // Story ID sẽ được set từ URL path variable
    private Long storyId;

    @NotNull(message = "Số chapter không được để trống")
    @Min(value = 1, message = "Số chapter phải lớn hơn 0")
    @Max(value = 10000, message = "Số chapter không được vượt quá 10,000")
    private Integer chapterNumber;

    @NotBlank(message = "Slug không được để trống")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug chỉ được chứa chữ thường, số và dấu gạch ngang")
    @Size(min = 1, max = 200, message = "Slug phải từ 1-200 ký tự")
    private String slug;

    @Size(max = 500, message = "Tiêu đề không được quá 500 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(min = 50, max = 1000000, message = "Nội dung phải từ 50-1,000,000 ký tự")
    private String content;
}
