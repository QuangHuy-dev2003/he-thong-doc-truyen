package com.meobeo.truyen.domain.request.subscription;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddSubscriptionRequest {

    @NotNull(message = "ID truyện không được để trống")
    private Long storyId;
}
