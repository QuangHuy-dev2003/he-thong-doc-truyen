package com.meobeo.truyen.domain.request.chapter;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateChapterRequest {
    // Chapter number được xác định từ URL, không cần trong request body

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
