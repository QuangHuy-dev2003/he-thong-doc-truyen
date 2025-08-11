package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Chapter;
import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.request.chapter.CreateChapterRequest;
import com.meobeo.truyen.domain.request.chapter.UpdateChapterRequest;
import com.meobeo.truyen.domain.response.chapter.ChapterResponse;
import com.meobeo.truyen.domain.response.chapter.ChapterSummaryDto;
import com.meobeo.truyen.domain.response.chapter.ChapterListResponse;
import com.meobeo.truyen.exception.ForbiddenException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.mapper.ChapterMapper;
import com.meobeo.truyen.repository.ChapterRepository;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.service.interfaces.ChapterService;
import com.meobeo.truyen.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final ChapterMapper chapterMapper;
    private final SecurityUtils securityUtils;

    @Override
    public ChapterResponse createChapter(CreateChapterRequest request, Long userId) {
        log.info("Tạo chapter mới: title={}, storyId={}, userId={}",
                request.getTitle(), request.getStoryId(), userId);

        // Lấy story và kiểm tra quyền
        Story story = getStoryById(request.getStoryId());
        validateEditPermissions(story, userId);

        // Validate slug
        if (isSlugExists(request.getSlug())) {
            throw new IllegalArgumentException("Slug đã tồn tại: " + request.getSlug());
        }

        // Validate chapter number
        if (isChapterNumberExists(request.getStoryId(), request.getChapterNumber())) {
            throw new IllegalArgumentException("Số chapter đã tồn tại: " + request.getChapterNumber());
        }

        // Tạo chapter mới
        Chapter chapter = new Chapter();
        chapter.setTitle(request.getTitle());
        chapter.setSlug(request.getSlug());
        chapter.setChapterNumber(request.getChapterNumber());
        chapter.setContent(request.getContent());
        chapter.setStory(story);

        Chapter savedChapter = chapterRepository.save(chapter);
        log.info("Tạo chapter thành công: chapterId={}", savedChapter.getId());

        return chapterMapper.toChapterResponse(savedChapter);
    }

    @Override
    public ChapterResponse updateChapterByStoryAndNumber(Long storyId, Integer chapterNumber,
            UpdateChapterRequest request, Long userId) {
        log.info("Cập nhật chapter theo story và number: storyId={}, chapterNumber={}, userId={}", storyId,
                chapterNumber, userId);

        // Lấy chapter theo storyId và chapterNumber
        Chapter chapter = getChapterByStoryAndNumber(storyId, chapterNumber);
        validateEditPermissions(chapter.getStory(), userId);

        // Validate slug nếu có thay đổi (không cho phép update chapterNumber nữa)
        if (!chapter.getSlug().equals(request.getSlug()) && isSlugExists(request.getSlug(), chapter.getId())) {
            throw new IllegalArgumentException("Slug đã tồn tại: " + request.getSlug());
        }

        // Cập nhật thông tin chapter (không update chapterNumber)
        chapter.setTitle(request.getTitle());
        chapter.setSlug(request.getSlug());
        chapter.setContent(request.getContent());
        // Không update chapterNumber vì đã xác định bằng URL

        Chapter updatedChapter = chapterRepository.save(chapter);
        log.info("Cập nhật chapter thành công: chapterId={}", updatedChapter.getId());

        return chapterMapper.toChapterResponse(updatedChapter);
    }

    @Override
    public void deleteChapter(Long chapterId, Long userId) {
        log.info("Xóa chapter: chapterId={}, userId={}", chapterId, userId);

        // Lấy chapter và kiểm tra quyền
        Chapter chapter = getChapterById(chapterId);
        validateEditPermissions(chapter.getStory(), userId);

        // Xóa chapter (cascade sẽ xóa các bảng liên quan)
        chapterRepository.delete(chapter);
        log.info("Xóa chapter thành công: chapterId={}", chapterId);
    }

    @Override
    public void deleteChapterByStoryAndNumber(Long storyId, Integer chapterNumber, Long userId) {
        log.info("Xóa chapter theo story và number: storyId={}, chapterNumber={}, userId={}", storyId, chapterNumber,
                userId);

        // Lấy chapter theo storyId và chapterNumber
        Chapter chapter = getChapterByStoryAndNumber(storyId, chapterNumber);
        validateEditPermissions(chapter.getStory(), userId);

        // Xóa chapter (cascade sẽ xóa các bảng liên quan)
        chapterRepository.delete(chapter);
        log.info("Xóa chapter thành công: storyId={}, chapterNumber={}", storyId, chapterNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterResponse getChapterDetail(String identifier, Long userId) {
        log.info("Lấy chi tiết chapter: identifier={}, userId={}", identifier, userId);

        Chapter chapter;
        try {
            // Thử parse thành ID
            Long chapterId = Long.parseLong(identifier);
            chapter = getChapterById(chapterId);
        } catch (NumberFormatException e) {
            // Nếu không phải số thì tìm theo slug
            chapter = getChapterBySlug(identifier);
        }

        // Kiểm tra quyền xem chapter (nếu cần)
        validateViewPermissions(chapter, userId);

        return chapterMapper.toChapterResponse(chapter);
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterResponse getChapterDetailByStoryAndNumber(Long storyId, Integer chapterNumber, Long userId) {
        log.info("Lấy chi tiết chapter theo story và number: storyId={}, chapterNumber={}, userId={}",
                storyId, chapterNumber, userId);

        Chapter chapter = getChapterByStoryAndNumber(storyId, chapterNumber);

        // Kiểm tra quyền xem chapter (nếu cần)
        validateViewPermissions(chapter, userId);

        return chapterMapper.toChapterResponse(chapter);
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterListResponse getChaptersByStory(String storyIdentifier, Pageable pageable) {
        log.info("Lấy danh sách chapter của truyện: storyIdentifier={}", storyIdentifier);

        Page<Chapter> chapterPage;
        Story story = null;

        try {
            // Thử parse thành ID
            Long storyId = Long.parseLong(storyIdentifier);
            chapterPage = chapterRepository.findByStoryIdOrderByChapterNumber(storyId, pageable);
            // Lấy thông tin story
            if (!chapterPage.isEmpty()) {
                story = chapterPage.getContent().get(0).getStory();
            } else {
                story = getStoryById(storyId);
            }
        } catch (NumberFormatException e) {
            // Nếu không phải số thì tìm theo slug
            chapterPage = chapterRepository.findByStorySlugOrderByChapterNumber(storyIdentifier, pageable);
            // Lấy thông tin story
            if (!chapterPage.isEmpty()) {
                story = chapterPage.getContent().get(0).getStory();
            } else {
                story = getStoryBySlug(storyIdentifier);
            }
        }

        // Convert thành ChapterSummaryDto (không có content)
        Page<ChapterSummaryDto> summaryPage = chapterPage.map(chapterMapper::toChapterSummaryDto);

        return ChapterListResponse.fromPageWithStoryInfo(summaryPage,
                story.getId(), story.getTitle(), story.getSlug());
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterResponse getNextChapter(Long chapterId) {
        log.info("Lấy chapter tiếp theo: chapterId={}", chapterId);

        Chapter currentChapter = getChapterById(chapterId);
        Integer nextNumber = currentChapter.getChapterNumber() + 1;

        return chapterRepository
                .findByStoryIdAndChapterNumber(currentChapter.getStory().getId(), nextNumber)
                .map(chapterMapper::toChapterResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterResponse getPreviousChapter(Long chapterId) {
        log.info("Lấy chapter trước đó: chapterId={}", chapterId);

        Chapter currentChapter = getChapterById(chapterId);
        Integer prevNumber = currentChapter.getChapterNumber() - 1;
        if (prevNumber <= 0)
            return null;

        return chapterRepository
                .findByStoryIdAndChapterNumber(currentChapter.getStory().getId(), prevNumber)
                .map(chapterMapper::toChapterResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterResponse getNextChapterByStoryAndNumber(Long storyId, Integer chapterNumber) {
        log.info("Lấy chapter tiếp theo theo story và number: storyId={}, chapterNumber={}", storyId, chapterNumber);

        Integer nextNumber = chapterNumber + 1;
        return chapterRepository
                .findByStoryIdAndChapterNumber(storyId, nextNumber)
                .map(chapterMapper::toChapterResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterResponse getPreviousChapterByStoryAndNumber(Long storyId, Integer chapterNumber) {
        log.info("Lấy chapter trước đó theo story và number: storyId={}, chapterNumber={}", storyId, chapterNumber);

        Integer prevNumber = chapterNumber - 1;
        if (prevNumber <= 0)
            return null;

        return chapterRepository
                .findByStoryIdAndChapterNumber(storyId, prevNumber)
                .map(chapterMapper::toChapterResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEditChapter(Long chapterId, Long userId) {
        try {
            Chapter chapter = getChapterById(chapterId);
            return validateEditPermissions(chapter.getStory(), userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlugExists(String slug) {
        return chapterRepository.existsBySlug(slug);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlugExists(String slug, Long excludeChapterId) {
        return chapterRepository.existsBySlugAndIdNot(slug, excludeChapterId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isChapterNumberExists(Long storyId, Integer chapterNumber) {
        return chapterRepository.existsByStoryIdAndChapterNumber(storyId, chapterNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isChapterNumberExists(Long storyId, Integer chapterNumber, Long excludeChapterId) {
        return chapterRepository.existsByStoryIdAndChapterNumberAndIdNot(storyId, chapterNumber, excludeChapterId);
    }

    // Helper methods
    private Story getStoryById(Long storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện: " + storyId));
    }

    private Story getStoryBySlug(String slug) {
        return storyRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện: " + slug));
    }

    private Chapter getChapterById(Long chapterId) {
        return chapterRepository.findByIdWithStory(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chapter: " + chapterId));
    }

    private Chapter getChapterBySlug(String slug) {
        return chapterRepository.findBySlugWithStory(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chapter: " + slug));
    }

    private Chapter getChapterByStoryAndNumber(Long storyId, Integer chapterNumber) {
        return chapterRepository.findByStoryIdAndChapterNumber(storyId, chapterNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Không tìm thấy chapter %d của story %d", chapterNumber, storyId)));
    }

    private boolean validateEditPermissions(Story story, Long userId) {
        // Admin có thể sửa mọi chapter
        if (securityUtils.isAdmin()) {
            return true;
        }

        // Tác giả chỉ có thể sửa chapter của truyện mình
        if (!story.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Không có quyền chỉnh sửa chapter này");
        }

        return true;
    }

    private void validateViewPermissions(Chapter chapter, Long userId) {
        // TODO: Implement logic kiểm tra quyền xem chapter bị khóa
        // Hiện tại cho phép xem tất cả chapter
    }
}
