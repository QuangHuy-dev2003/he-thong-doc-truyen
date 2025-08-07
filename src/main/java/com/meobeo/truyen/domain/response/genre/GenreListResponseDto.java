package com.meobeo.truyen.domain.response.genre;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class GenreListResponseDto {
    private List<GenreResponseDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    
    public static GenreListResponseDto fromPage(Page<GenreResponseDto> page) {
        GenreListResponseDto response = new GenreListResponseDto();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
        return response;
    }
} 