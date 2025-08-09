package com.meobeo.truyen.domain.request.story;

import com.meobeo.truyen.domain.enums.StoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Data
public class UpdateStoryRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 1, max = 255, message = "Tiêu đề phải từ 1-255 ký tự")
    private String title;

    @NotBlank(message = "Slug không được để trống")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug chỉ được chứa chữ thường, số và dấu gạch ngang")
    @Size(min = 1, max = 100, message = "Slug phải từ 1-100 ký tự")
    private String slug;

    @Size(max = 2000, message = "Mô tả không được quá 2000 ký tự")
    private String description;

    // THÊM FIELD MỚI
    @NotBlank(message = "Tên tác giả không được để trống")
    @Size(min = 1, max = 255, message = "Tên tác giả phải từ 1-255 ký tự")
    private String authorName;

    private StoryStatus status;

    @NotEmpty(message = "Phải chọn ít nhất 1 thể loại")
    private Set<Long> genreIds;

    private MultipartFile coverImage;
}