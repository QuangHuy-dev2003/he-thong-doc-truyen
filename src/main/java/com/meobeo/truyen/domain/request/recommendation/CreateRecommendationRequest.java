package com.meobeo.truyen.domain.request.recommendation;

import com.meobeo.truyen.domain.enums.RecommendationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRecommendationRequest {

    @NotNull(message = "ID truyện không được để trống")
    private Long storyId;

    @NotNull(message = "Loại đề cử không được để trống")
    private RecommendationType recommendationType;

    private String message;
}
