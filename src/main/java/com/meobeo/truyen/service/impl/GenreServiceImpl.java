package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Genre;
import com.meobeo.truyen.domain.request.genre.CreateGenreDto;
import com.meobeo.truyen.domain.request.genre.UpdateGenreDto;
import com.meobeo.truyen.domain.response.genre.GenreListResponseDto;
import com.meobeo.truyen.domain.response.genre.GenreResponseDto;
import com.meobeo.truyen.exception.GenreAlreadyExistsException;
import com.meobeo.truyen.exception.GenreNotFoundException;
import com.meobeo.truyen.mapper.GenreMapper;
import com.meobeo.truyen.repository.GenreRepository;
import com.meobeo.truyen.service.interfaces.GenreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    @Override
    public GenreResponseDto createGenre(CreateGenreDto createGenreDto) {
        log.info("Tạo thể loại mới: {}", createGenreDto.getName());

        // Kiểm tra thể loại đã tồn tại chưa
        if (genreRepository.existsByName(createGenreDto.getName())) {
            throw new GenreAlreadyExistsException("Thể loại '" + createGenreDto.getName() + "' đã tồn tại");
        }

        Genre genre = new Genre();
        genre.setName(createGenreDto.getName());

        Genre savedGenre = genreRepository.save(genre);
        log.info("Đã tạo thể loại thành công với ID: {}", savedGenre.getId());

        return genreMapper.toGenreResponseDto(savedGenre);
    }

    @Override
    public GenreResponseDto updateGenre(Long id, UpdateGenreDto updateGenreDto) {
        log.info("Cập nhật thể loại với ID: {}", id);

        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new GenreNotFoundException("Không tìm thấy thể loại với ID: " + id));

        // Kiểm tra nếu tên mới đã tồn tại (trừ chính nó)
        if (!genre.getName().equals(updateGenreDto.getName()) &&
                genreRepository.existsByName(updateGenreDto.getName())) {
            throw new GenreAlreadyExistsException("Thể loại '" + updateGenreDto.getName() + "' đã tồn tại");
        }

        genre.setName(updateGenreDto.getName());
        Genre updatedGenre = genreRepository.save(genre);
        log.info("Đã cập nhật thể loại thành công với ID: {}", id);

        return genreMapper.toGenreResponseDto(updatedGenre);
    }

    @Override
    @Transactional(readOnly = true)
    public GenreResponseDto getGenreById(Long id) {
        log.info("Lấy thông tin thể loại với ID: {}", id);

        Genre genre = genreRepository.findByIdWithStories(id)
                .orElseThrow(() -> new GenreNotFoundException("Không tìm thấy thể loại với ID: " + id));

        return genreMapper.toGenreResponseDto(genre);
    }

    @Override
    @Transactional(readOnly = true)
    public GenreListResponseDto getAllGenres(Pageable pageable) {
        log.info("Lấy danh sách thể loại với phân trang: page={}, size={}", pageable.getPageNumber(),
                pageable.getPageSize());

        Page<Genre> genrePage = genreRepository.findAllOrderByName(pageable);
        Page<GenreResponseDto> dtoPage = genrePage.map(genreMapper::toGenreResponseDto);

        return GenreListResponseDto.fromPage(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public GenreListResponseDto searchGenres(String keyword, Pageable pageable) {
        log.info("Tìm kiếm thể loại với từ khóa: '{}'", keyword);

        Page<Genre> genrePage = genreRepository.findByNameContainingIgnoreCase(keyword, pageable);
        Page<GenreResponseDto> dtoPage = genrePage.map(genreMapper::toGenreResponseDto);

        return GenreListResponseDto.fromPage(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenreResponseDto> getAllGenresForDropdown() {
        log.info("Lấy danh sách thể loại cho dropdown");

        List<Genre> genres = genreRepository.findAllWithStories();
        return genres.stream()
                .map(genreMapper::toGenreResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteGenre(Long id) {
        log.info("Xóa thể loại với ID: {}", id);

        Genre genre = genreRepository.findByIdWithStories(id)
                .orElseThrow(() -> new GenreNotFoundException("Không tìm thấy thể loại với ID: " + id));

        // Kiểm tra xem thể loại có truyện nào không
        if (!genre.getStories().isEmpty()) {
            throw new RuntimeException(
                    "Không thể xóa thể loại đang có truyện. Vui lòng xóa hoặc chuyển truyện sang thể loại khác trước.");
        }

        genreRepository.delete(genre);
        log.info("Đã xóa thể loại thành công với ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return genreRepository.existsByName(name);
    }
}