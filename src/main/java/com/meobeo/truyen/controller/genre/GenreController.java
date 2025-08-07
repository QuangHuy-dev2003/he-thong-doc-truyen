package com.meobeo.truyen.controller.genre;

import com.meobeo.truyen.domain.request.genre.CreateGenreDto;
import com.meobeo.truyen.domain.request.genre.UpdateGenreDto;
import com.meobeo.truyen.domain.response.genre.GenreListResponseDto;
import com.meobeo.truyen.domain.response.genre.GenreResponseDto;
import com.meobeo.truyen.service.interfaces.GenreService;
import com.meobeo.truyen.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class GenreController {

    private final GenreService genreService;

    @PostMapping("/genres/create")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<GenreResponseDto>> createGenre(
            @Valid @RequestBody CreateGenreDto createGenreDto) {
        log.info("Nhận request tạo thể loại: {}", createGenreDto.getName());

        GenreResponseDto genreResponse = genreService.createGenre(createGenreDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thể loại thành công", genreResponse));
    }

    @PutMapping("/genres/update/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<GenreResponseDto>> updateGenre(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGenreDto updateGenreDto) {
        log.info("Nhận request cập nhật thể loại với ID: {}", id);

        // Validate ID
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ID thể loại không hợp lệ"));
        }

        GenreResponseDto genreResponse = genreService.updateGenre(id, updateGenreDto);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật thể loại thành công", genreResponse));
    }

    @GetMapping("/genres/{id}")
    public ResponseEntity<ApiResponse<GenreResponseDto>> getGenreById(@PathVariable Long id) {
        log.info("Nhận request lấy thông tin thể loại với ID: {}", id);

        // Validate ID
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ID thể loại không hợp lệ"));
        }

        GenreResponseDto genreResponse = genreService.getGenreById(id);

        return ResponseEntity.ok(ApiResponse.success(genreResponse));
    }

    @GetMapping("/genres/all")
    public ResponseEntity<ApiResponse<GenreListResponseDto>> getAllGenres(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Nhận request lấy danh sách thể loại: page={}, size={}, sortBy={}, sortDir={}",
                page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        GenreListResponseDto genreListResponse = genreService.getAllGenres(pageable);

        return ResponseEntity.ok(ApiResponse.success(genreListResponse));
    }

    @GetMapping("/genres/search")
    public ResponseEntity<ApiResponse<GenreListResponseDto>> searchGenres(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Nhận request tìm kiếm thể loại với từ khóa: '{}'", keyword);

        // Validate keyword length
        if (keyword != null && keyword.length() > 100) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Từ khóa tìm kiếm không được vượt quá 100 ký tự"));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        GenreListResponseDto genreListResponse = genreService.searchGenres(keyword, pageable);

        return ResponseEntity.ok(ApiResponse.success(genreListResponse));
    }

    @GetMapping("/genres/dropdown")
    public ResponseEntity<ApiResponse<List<GenreResponseDto>>> getAllGenresForDropdown() {
        log.info("Nhận request lấy danh sách thể loại cho dropdown");

        List<GenreResponseDto> genres = genreService.getAllGenresForDropdown();

        return ResponseEntity.ok(ApiResponse.success(genres));
    }

    @DeleteMapping("/genres/delete/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteGenre(@PathVariable Long id) {
        log.info("Nhận request xóa thể loại với ID: {}", id);

        // Validate ID
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ID thể loại không hợp lệ"));
        }

        genreService.deleteGenre(id);

        return ResponseEntity.ok(ApiResponse.success("Xóa thể loại thành công", "Deleted"));
    }

    @GetMapping("/genres/check-name/{name}")
    public ResponseEntity<ApiResponse<Boolean>> checkGenreNameExists(@PathVariable String name) {
        log.info("Nhận request kiểm tra tên thể loại: {}", name);

        // Validate name
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Tên thể loại không được để trống"));
        }

        if (name.length() > 50) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Tên thể loại không được vượt quá 50 ký tự"));
        }

        boolean exists = genreService.existsByName(name.trim());

        return ResponseEntity.ok(ApiResponse.success(exists));
    }
}