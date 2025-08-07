package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.genre.CreateGenreDto;
import com.meobeo.truyen.domain.request.genre.UpdateGenreDto;
import com.meobeo.truyen.domain.response.genre.GenreListResponseDto;
import com.meobeo.truyen.domain.response.genre.GenreResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GenreService {

    GenreResponseDto createGenre(CreateGenreDto createGenreDto);

    GenreResponseDto updateGenre(Long id, UpdateGenreDto updateGenreDto);

    GenreResponseDto getGenreById(Long id);

    GenreListResponseDto getAllGenres(Pageable pageable);

    GenreListResponseDto searchGenres(String keyword, Pageable pageable);

    List<GenreResponseDto> getAllGenresForDropdown();

    void deleteGenre(Long id);

    boolean existsByName(String name);
}