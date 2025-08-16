package com.meobeo.truyen.domain.request.chapter;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnlockFullStoryRequest {

    @NotNull(message = "Story ID không được để trống")
    private Long storyId;

    private String description;
}
