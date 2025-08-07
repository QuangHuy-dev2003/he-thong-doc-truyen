package com.meobeo.truyen.domain.response.genre;

import lombok.Data;

@Data
public class GenreResponseDto {
    private Long id;
    private String name;
    private Long storyCount;
}