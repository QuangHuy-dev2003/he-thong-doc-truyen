package com.meobeo.truyen.domain.request.favorite;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToFavoriteRequest {

    @NotNull(message = "ID truyện không được để trống")
    private Long storyId;
}
