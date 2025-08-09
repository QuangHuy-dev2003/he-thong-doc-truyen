package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.entity.StoryComment;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.request.comment.CreateCommentRequest;
import com.meobeo.truyen.domain.request.comment.UpdateCommentRequest;
import com.meobeo.truyen.domain.response.comment.CommentResponse;
import com.meobeo.truyen.domain.response.comment.CommentListResponse;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.exception.ForbiddenException;
import com.meobeo.truyen.mapper.StoryCommentMapper;
import com.meobeo.truyen.repository.StoryCommentRepository;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.StoryCommentService;
import com.meobeo.truyen.utils.ContentFilterUtil;
import com.meobeo.truyen.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryCommentServiceImpl implements StoryCommentService {

    private final StoryCommentRepository storyCommentRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final StoryCommentMapper commentMapper;
    private final ContentFilterUtil contentFilterUtil;
    private final SecurityUtils securityUtils;

    // Rate limiting: không cho comment quá 3 lần trong 5 phút
    private static final int MAX_COMMENTS_PER_PERIOD = 3;
    private static final int RATE_LIMIT_MINUTES = 5;

    // Rate limiting cho update: không cho update quá 3 lần trong 5 phút
    private static final int MAX_UPDATES_PER_PERIOD = 3;
    private static final int UPDATE_RATE_LIMIT_MINUTES = 5;

    @Override
    @Transactional
    public CommentResponse createComment(Long storyId, CreateCommentRequest request, Long userId) {
        log.info("Tạo comment mới cho story: storyId={}, userId={}", storyId, userId);

        // Validate content filter
        contentFilterUtil.validateContent(request.getContent());

        // Lấy thông tin story
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + storyId));

        // Lấy thông tin user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + userId));

        // Kiểm tra rate limiting
        if (!canUserComment(userId, storyId)) {
            throw new IllegalArgumentException(
                    String.format("Bạn chỉ có thể comment tối đa %d lần trong %d phút. Vui lòng chờ một lúc.",
                            MAX_COMMENTS_PER_PERIOD, RATE_LIMIT_MINUTES));
        }

        // Tạo comment mới
        StoryComment comment = new StoryComment();
        comment.setContent(request.getContent().trim());
        comment.setStory(story);
        comment.setUser(user);

        // Save comment
        StoryComment savedComment = storyCommentRepository.save(comment);

        log.info("Đã tạo story comment thành công: commentId={}", savedComment.getId());

        return commentMapper.toCommentResponse(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentListResponse getCommentsByStory(Long storyId, Pageable pageable) {
        log.info("Lấy comment theo storyId={}, page={}, size={}",
                storyId, pageable.getPageNumber(), pageable.getPageSize());

        // Kiểm tra story tồn tại
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + storyId));

        // Lấy comments với phân trang
        Page<StoryComment> commentsPage = storyCommentRepository
                .findByStoryIdOrderByCreatedAtDesc(storyId, pageable);

        // Convert sang CommentResponse
        Page<CommentResponse> responsePage = commentsPage.map(commentMapper::toCommentResponse);

        // Đếm tổng số comment
        Long totalComments = storyCommentRepository.countByStoryId(storyId);

        return CommentListResponse.fromPage(
                responsePage,
                storyId,
                null, // Không có chapterId cho story comment
                null, // Không có chapterNumber cho story comment
                story.getTitle(), // Dùng story title
                totalComments);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request, Long userId) {
        log.info("Cập nhật story comment: commentId={}, userId={}", commentId, userId);

        // Validate content filter
        contentFilterUtil.validateContent(request.getContent());

        // Lấy comment với thông tin chi tiết
        StoryComment comment = storyCommentRepository.findByIdWithDetails(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy comment với ID: " + commentId));

        // Kiểm tra quyền sửa (chỉ admin hoặc người tạo comment)
        boolean isAdmin = securityUtils.hasRole("ADMIN");
        boolean isOwner = comment.getUser().getId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Bạn không có quyền sửa comment này");
        }

        // Kiểm tra rate limiting cho update (chỉ áp dụng cho user thường, admin không
        // bị giới hạn)
        if (!isAdmin && !canUserUpdate(userId)) {
            throw new IllegalArgumentException(
                    String.format("Bạn chỉ có thể sửa comment tối đa %d lần trong %d phút. Vui lòng chờ một lúc.",
                            MAX_UPDATES_PER_PERIOD, UPDATE_RATE_LIMIT_MINUTES));
        }

        // Cập nhật nội dung comment
        comment.setContent(request.getContent().trim());

        // Save comment (updatedAt sẽ được tự động cập nhật bởi @UpdateTimestamp)
        StoryComment updatedComment = storyCommentRepository.save(comment);

        log.info("Đã cập nhật story comment thành công: commentId={}", commentId);

        return commentMapper.toCommentResponse(updatedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        log.info("Xóa story comment: commentId={}, userId={}", commentId, userId);

        // Lấy comment
        StoryComment comment = storyCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy comment với ID: " + commentId));

        // Kiểm tra quyền xóa (chỉ admin hoặc người tạo comment)
        boolean isAdmin = securityUtils.hasRole("ADMIN");
        boolean isOwner = comment.getUser().getId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Bạn không có quyền xóa comment này");
        }

        // Xóa comment
        storyCommentRepository.delete(comment);

        log.info("Đã xóa story comment thành công: commentId={}", commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserComment(Long userId, Long storyId) {
        // Kiểm tra số lượng comment gần đây của user trong story này
        LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);
        Long recentComments = storyCommentRepository.countRecentCommentsByUserAndStory(userId, storyId,
                sinceTime);

        return recentComments < MAX_COMMENTS_PER_PERIOD;
    }

    @Override
    @Transactional(readOnly = true)
    public Long countCommentsByStory(Long storyId) {
        // Kiểm tra story tồn tại
        storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + storyId));

        return storyCommentRepository.countByStoryId(storyId);
    }

    /**
     * Kiểm tra user có thể update comment không (rate limiting)
     */
    @Transactional(readOnly = true)
    public boolean canUserUpdate(Long userId) {
        // Kiểm tra số lần update gần đây của user
        LocalDateTime sinceTime = LocalDateTime.now().minusMinutes(UPDATE_RATE_LIMIT_MINUTES);
        Long recentUpdates = storyCommentRepository.countRecentUpdatesByUser(userId, sinceTime);

        return recentUpdates < MAX_UPDATES_PER_PERIOD;
    }
}
