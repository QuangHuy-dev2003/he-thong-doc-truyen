package com.meobeo.truyen.domain.request.story;

import com.meobeo.truyen.domain.enums.StoryStatus;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

@Data
public class StorySearchRequest {

    private String search; // Tìm kiếm theo title, slug, author
    private Set<Long> genreIds; // Lọc theo thể loại
    private StoryStatus status; // Lọc theo trạng thái
    private String sortBy = "createdAt"; // Sắp xếp theo field
    private String sortDirection = "desc"; // asc hoặc desc
    private int page = 0;
    private int size = 20;

    public Pageable toPageable() {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        return PageRequest.of(page, size, sort);
    }

    public Pageable toPageableWithoutSort() {
        return PageRequest.of(page, size);
    }
}