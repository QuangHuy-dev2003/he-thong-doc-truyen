package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.Genre;
import com.meobeo.truyen.domain.response.genre.GenreResponseDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GenreMapper {

    @Transactional(readOnly = true)
    public GenreResponseDto toGenreResponseDto(Genre genre) {
        if (genre == null) {
            return null;
        }

        GenreResponseDto dto = new GenreResponseDto();
        dto.setId(genre.getId());
        dto.setName(genre.getName());

        // Đếm số lượng truyện trong thể loại này
        if (genre.getStories() != null) {
            dto.setStoryCount((long) genre.getStories().size());
        } else {
            dto.setStoryCount(0L);
        }

        return dto;
    }
}