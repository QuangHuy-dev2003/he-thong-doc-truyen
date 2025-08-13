package com.meobeo.truyen.domain.request.reading;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecordReadingRequest {

    @NotNull(message = "ID chapter không được để trống")
    private Long chapterId;
}
