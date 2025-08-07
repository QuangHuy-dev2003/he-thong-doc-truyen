package com.meobeo.truyen.domain.request.genre;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateGenreDto {

    @NotBlank(message = "Tên thể loại không được để trống")
    @Size(min = 2, max = 50, message = "Tên thể loại phải từ 2 đến 50 ký tự")
    private String name;
}