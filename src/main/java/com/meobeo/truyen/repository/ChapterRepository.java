package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    /**
     * Lấy 5 chapter mới nhất của truyện
     */
    List<Chapter> findTop5ByStoryIdOrderByChapterNumberDesc(Long storyId);
}