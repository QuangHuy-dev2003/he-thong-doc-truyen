package com.meobeo.truyen.domain.request.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCommentRequest {

    @NotBlank(message = "Nội dung comment không được để trống")
    @Size(min = 1, max = 1000, message = "Nội dung comment phải từ 1-1000 ký tự")
    private String content;
}
