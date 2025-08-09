package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Chapter;
import com.meobeo.truyen.domain.entity.ChapterComment;
import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.comment.CreateCommentRequest;
import com.meobeo.truyen.domain.response.comment.CommentResponse;
import com.meobeo.truyen.domain.response.comment.CommentListResponse;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.exception.ForbiddenException;
import com.meobeo.truyen.mapper.ChapterCommentMapper;
import com.meobeo.truyen.repository.ChapterCommentRepository;
import com.meobeo.truyen.repository.ChapterRepository;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.ChapterCommentService;
import com.meobeo.truyen.utils.ContentFilterUtil;
import com.meobeo.truyen.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterCommentServiceImpl implements ChapterCommentService {

    private final ChapterCommentRepository chapterCommentRepository;
    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final ChapterCommentMapper commentMapper;
    private final ContentFilterUtil contentFilterUtil;
    private final SecurityUtils securityUtils;

    // Rate limiting: không cho comment quá 3 lần trong 5 phút
    private static final int MAX_COMMENTS_PER_PERIOD = 3;
    private static final int RATE_LIMIT_MINUTES = 5;

    @Override
    @Transactional
    public CommentResponse createComment(Long storyId, Integer chapterNumber, CreateCommentRequest request,
            Long userId) {
        log.info("Tạo comment mới: storyId={}, chapterNumber={}, userId={}", storyId, chapterNumber, userId);

        // Validate content filter
        contentFilterUtil.validateContent(request.getContent());

        // Lấy thông tin story
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + storyId));

        // Lấy thông tin chapter theo storyId và chapterNumber
        Chapter chapter = chapterRepository.findByStoryIdAndChapterNumber(storyId, chapterNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Không tìm thấy chapter %d của truyện ID %d", chapterNumber, storyId)));

        // Lấy thông tin user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + userId));

        // Kiểm tra rate limiting
        if (!canUserComment(userId, chapter.getId())) {
            throw new IllegalArgumentException(
                    String.format("Bạn chỉ có thể comment tối đa %d lần trong %d phút. Vui lòng chờ một lúc.",
                            MAX_COMMENTS_PER_PERIOD, RATE_LIMIT_MINUTES));
        }

        // Tạo comment mới
        ChapterComment comment = new ChapterComment();
        comment.setContent(request.getContent().trim());
        comment.setStory(story);
        comment.setChapter(chapter);
        comment.setUser(user);

        // Save comment
        ChapterComment savedComment = chapterCommentRepository.save(comment);

        log.info("Đã tạo comment thành công: commentId={}", savedComment.getId());

        return commentMapper.toCommentResponse(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentListResponse getCommentsByStoryAndChapter(Long storyId, Integer chapterNumber, Pageable pageable) {
        log.info("Lấy comment theo storyId={}, chapterNumber={}, page={}, size={}",
                storyId, chapterNumber, pageable.getPageNumber(), pageable.getPageSize());

        // Kiểm tra story tồn tại
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + storyId));

        // Kiểm tra chapter tồn tại
        Chapter chapter = chapterRepository.findByStoryIdAndChapterNumber(storyId, chapterNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Không tìm thấy chapter %d của truyện ID %d", chapterNumber, storyId)));

        // Lấy comments với phân trang
        Page<ChapterComment> commentsPage = chapterCommentRepository
                .findByStoryIdAndChapterNumberOrderByCreatedAtDesc(storyId, chapterNumber, pageable);

        // Convert sang CommentResponse
        List<CommentResponse> commentResponses = commentsPage.getContent().stream()
                .map(commentMapper::toCommentResponse)
                .toList();

        // Tạo Page<CommentResponse> để dùng trong CommentListResponse
        Page<CommentResponse> responsePage = commentsPage.map(commentMapper::toCommentResponse);

        // Đếm tổng số comment
        Long totalComments = chapterCommentRepository.countByStoryIdAndChapterNumber(storyId, chapterNumber);

        return CommentListResponse.fromPage(
                responsePage,
                storyId,
                chapter.getId(),
                chapterNumber,
                chapter.getTitle(),
                totalComments);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentListResponse getCommentsByChapter(Long chapterId, Pageable pageable) {
        log.info("Lấy comment theo chapterId={}, page={}, size={}",
                chapterId, pageable.getPageNumber(), pageable.getPageSize());

        // Kiểm tra chapter tồn tại
        Chapter chapter = chapterRepository.findByIdWithStory(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chapter với ID: " + chapterId));

        // Lấy comments với phân trang
        Page<ChapterComment> commentsPage = chapterCommentRepository
                .findByStoryIdAndChapterIdOrderByCreatedAtDesc(chapter.getStory().getId(), chapterId, pageable);

        // Convert sang CommentResponse
        Page<CommentResponse> responsePage = commentsPage.map(commentMapper::toCommentResponse);

        // Đếm tổng số comment
        Long totalComments = chapterCommentRepository.countByStoryIdAndChapterId(chapter.getStory().getId(), chapterId);

        return CommentListResponse.fromPage(
                responsePage,
                chapter.getStory().getId(),
                chapterId,
                chapter.getChapterNumber(),
                chapter.getTitle(),
                totalComments);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        log.info("Xóa comment: commentId={}, userId={}", commentId, userId);

        // Lấy comment
        ChapterComment comment = chapterCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy comment với ID: " + commentId));

        // Kiểm tra quyền xóa (chỉ admin hoặc người tạo comment)
        boolean isAdmin = securityUtils.hasRole("ADMIN");
        boolean isOwner = comment.getUser().getId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Bạn không có quyền xóa comment này");
        }

        // Xóa comment
        chapterCommentRepository.delete(comment);

        log.info("Đã xóa comment thành công: commentId={}", commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserComment(Long userId, Long chapterId) {
        // Kiểm tra số lượng comment gần đây của user trong chapter này
        LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);
        Long recentComments = chapterCommentRepository.countRecentCommentsByUserAndChapter(userId, chapterId,
                sinceTime);

        return recentComments < MAX_COMMENTS_PER_PERIOD;
    }

    @Override
    @Transactional(readOnly = true)
    public Long countCommentsByChapter(Long chapterId) {
        Chapter chapter = chapterRepository.findByIdWithStory(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chapter với ID: " + chapterId));

        return chapterCommentRepository.countByStoryIdAndChapterId(chapter.getStory().getId(), chapterId);
    }
}
