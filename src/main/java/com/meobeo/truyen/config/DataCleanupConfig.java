package com.meobeo.truyen.config;

import com.meobeo.truyen.domain.entity.Chapter;
import com.meobeo.truyen.repository.ChapterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!prod") // Chỉ chạy trong môi trường dev/test
public class DataCleanupConfig {

    private final ChapterRepository chapterRepository;

    // @Bean
    // public CommandLineRunner cleanupDuplicateChapters() {
    // return args -> {
    // log.info("Bắt đầu kiểm tra và dọn dẹp duplicate chapters...");

    // // Lấy tất cả chapters
    // List<Chapter> allChapters = chapterRepository.findAll();

    // // Nhóm theo story_id và chapter_number
    // Map<String, List<Chapter>> groupedChapters = allChapters.stream()
    // .collect(Collectors
    // .groupingBy(chapter -> chapter.getStory().getId() + "_" +
    // chapter.getChapterNumber()));

    // // Kiểm tra và xóa duplicate
    // int deletedCount = 0;
    // for (Map.Entry<String, List<Chapter>> entry : groupedChapters.entrySet()) {
    // List<Chapter> chapters = entry.getValue();
    // if (chapters.size() > 1) {
    // log.warn("Tìm thấy {} duplicate chapters cho key: {}", chapters.size(),
    // entry.getKey());

    // // Giữ lại chapter đầu tiên (có ID nhỏ nhất), xóa các chapter còn lại
    // Chapter keepChapter = chapters.stream()
    // .min((c1, c2) -> Long.compare(c1.getId(), c2.getId()))
    // .orElse(chapters.get(0));

    // for (Chapter chapter : chapters) {
    // if (!chapter.getId().equals(keepChapter.getId())) {
    // log.info("Xóa duplicate chapter: ID={}, Story={}, ChapterNumber={}",
    // chapter.getId(), chapter.getStory().getId(), chapter.getChapterNumber());
    // chapterRepository.delete(chapter);
    // deletedCount++;
    // }
    // }
    // }
    // }

    // if (deletedCount > 0) {
    // log.info("Đã xóa {} duplicate chapters", deletedCount);
    // } else {
    // log.info("Không tìm thấy duplicate chapters");
    // }
    // };
    // }
}
