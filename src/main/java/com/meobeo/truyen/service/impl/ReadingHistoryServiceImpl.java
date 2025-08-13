package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Chapter;
import com.meobeo.truyen.domain.entity.ReadingHistory;
import com.meobeo.truyen.domain.entity.ReadingHistoryId;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.response.reading.ReadingHistoryListResponse;
import com.meobeo.truyen.domain.response.reading.ReadingHistoryResponse;
import com.meobeo.truyen.domain.response.reading.LastReadChapterResponse;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.mapper.ReadingHistoryMapper;
import com.meobeo.truyen.repository.ChapterRepository;
import com.meobeo.truyen.repository.ReadingHistoryRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.ReadingHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadingHistoryServiceImpl implements ReadingHistoryService {

    private final ReadingHistoryRepository readingHistoryRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final ReadingHistoryMapper readingHistoryMapper;

    @Override
    public ReadingHistoryResponse recordReading(Long chapterId, Long userId) {
        log.info("Ghi lại lịch sử đọc: chapterId={}, userId={}", chapterId, userId);

        // Kiểm tra user và chapter tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + userId));

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chapter với ID: " + chapterId));

        Long storyId = chapter.getStory().getId();

        // Kiểm tra xem đã có lịch sử đọc cho story này chưa
        Optional<ReadingHistory> existingHistory = readingHistoryRepository.findByUserIdAndStoryId(userId, storyId);

        ReadingHistory readingHistory;
        if (existingHistory.isPresent()) {
            // Cập nhật chapter mới nhất
            readingHistory = existingHistory.get();
            readingHistory.setChapterId(chapterId);
            readingHistory.setChapter(chapter);
            readingHistory.setLastReadAt(LocalDateTime.now());
            log.info("Cập nhật lịch sử đọc: storyId={}, chapterId={}", storyId, chapterId);
        } else {
            // Tạo mới lịch sử đọc
            readingHistory = new ReadingHistory();
            ReadingHistoryId id = new ReadingHistoryId();
            id.setUserId(userId);
            id.setStoryId(storyId);
            readingHistory.setId(id);
            readingHistory.setUser(user);
            readingHistory.setChapterId(chapterId);
            readingHistory.setChapter(chapter);
            readingHistory.setStory(chapter.getStory());
            readingHistory.setLastReadAt(LocalDateTime.now());
            log.info("Tạo mới lịch sử đọc: storyId={}, chapterId={}", storyId, chapterId);
        }

        // Lưu vào database
        readingHistory = readingHistoryRepository.save(readingHistory);

        log.info("Đã ghi lại lịch sử đọc thành công: chapterId={}, userId={}, storyId={}", chapterId, userId, storyId);

        return readingHistoryMapper.toReadingHistoryResponse(readingHistory);
    }

    @Override
    @Transactional(readOnly = true)
    public ReadingHistoryListResponse getUserReadingHistory(Long userId, Pageable pageable) {
        log.info("Lấy lịch sử đọc của user: userId={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        // Kiểm tra user tồn tại
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy user với ID: " + userId);
        }

        Page<ReadingHistory> readingHistoryPage = readingHistoryRepository.findByUserIdWithFetch(userId, pageable);

        ReadingHistoryListResponse response = new ReadingHistoryListResponse();
        response.setContent(readingHistoryPage.getContent().stream()
                .map(readingHistoryMapper::toReadingHistoryResponse)
                .toList());
        response.setPage(readingHistoryPage.getNumber());
        response.setSize(readingHistoryPage.getSize());
        response.setTotalElements(readingHistoryPage.getTotalElements());
        response.setTotalPages(readingHistoryPage.getTotalPages());
        response.setHasNext(readingHistoryPage.hasNext());
        response.setHasPrevious(readingHistoryPage.hasPrevious());

        log.info("Đã lấy lịch sử đọc thành công: userId={}, total={}",
                userId, readingHistoryPage.getTotalElements());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ReadingHistoryResponse getUserReadingHistoryByStory(Long userId, Long storyId) {
        log.info("Lấy lịch sử đọc của user trong story: userId={}, storyId={}", userId, storyId);

        // Kiểm tra user tồn tại
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy user với ID: " + userId);
        }

        return readingHistoryRepository.findByUserIdAndStoryIdWithFetch(userId, storyId)
                .map(readingHistoryMapper::toReadingHistoryResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public LastReadChapterResponse getLastReadChapter(Long userId, Long storyId) {
        log.info("Lấy chapter cuối cùng đã đọc: userId={}, storyId={}", userId, storyId);

        // Kiểm tra user tồn tại
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy user với ID: " + userId);
        }

        // Lấy chapter cuối cùng đã đọc trong story
        return readingHistoryRepository.findLastReadChapterByUserAndStory(userId, storyId)
                .map(readingHistoryMapper::toLastReadChapterResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countUserReadStories(Long userId) {
        return readingHistoryRepository.countByUserId(userId);
    }

    @Override
    public void deleteReadingHistory(Long userId, Long storyId) {
        log.info("Xóa lịch sử đọc: userId={}, storyId={}", userId, storyId);

        if (!readingHistoryRepository.existsByUserIdAndStoryId(userId, storyId)) {
            throw new ResourceNotFoundException("Không tìm thấy lịch sử đọc");
        }

        readingHistoryRepository.deleteByUserIdAndStoryId(userId, storyId);
        log.info("Đã xóa lịch sử đọc thành công: userId={}, storyId={}", userId, storyId);
    }

    @Override
    public void clearUserReadingHistory(Long userId) {
        log.info("Xóa tất cả lịch sử đọc của user: userId={}", userId);

        readingHistoryRepository.deleteAllByUserId(userId);
        log.info("Đã xóa tất cả lịch sử đọc thành công: userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserReadStory(Long userId, Long storyId) {
        return readingHistoryRepository.existsByUserIdAndStoryId(userId, storyId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReadingHistoryListResponse getLastReadStoriesByUser(Long userId) {
        log.info("Lấy danh sách story đã đọc của user: userId={}", userId);

        // Kiểm tra user tồn tại
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy user với ID: " + userId);
        }

        List<ReadingHistory> lastReadStories = readingHistoryRepository.findLastReadStoriesByUser(userId);

        ReadingHistoryListResponse response = new ReadingHistoryListResponse();
        response.setContent(lastReadStories.stream()
                .map(readingHistoryMapper::toReadingHistoryResponse)
                .toList());
        response.setPage(0);
        response.setSize(lastReadStories.size());
        response.setTotalElements((long) lastReadStories.size());
        response.setTotalPages(1);
        response.setHasNext(false);
        response.setHasPrevious(false);

        log.info("Đã lấy danh sách story đã đọc thành công: userId={}, count={}",
                userId, lastReadStories.size());

        return response;
    }
}
