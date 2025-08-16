package com.meobeo.truyen.domain.request.chapter;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnlockChapterRequest {

    @NotNull(message = "Chapter ID không được để trống")
    private Long chapterId;

    private String description;
}
