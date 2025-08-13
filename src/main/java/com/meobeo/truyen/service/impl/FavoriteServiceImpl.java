package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Favorite;
import com.meobeo.truyen.domain.entity.FavoriteId;
import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.response.favorite.FavoriteListResponse;
import com.meobeo.truyen.domain.response.favorite.FavoriteResponse;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.exception.UserAlreadyExistsException;
import com.meobeo.truyen.mapper.FavoriteMapper;
import com.meobeo.truyen.repository.FavoriteRepository;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.FavoriteService;
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
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final FavoriteMapper favoriteMapper;

    @Override
    public FavoriteResponse addToFavorite(Long storyId, Long userId) {
        log.info("Thêm truyện vào yêu thích: storyId={}, userId={}", storyId, userId);

        // Kiểm tra user và story tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + userId));

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + storyId));

        // Kiểm tra đã yêu thích chưa
        if (favoriteRepository.existsByUserIdAndStoryId(userId, storyId)) {
            throw new UserAlreadyExistsException("Bạn đã yêu thích truyện này rồi");
        }

        // Tạo favorite mới
        Favorite favorite = new Favorite();
        FavoriteId favoriteId = new FavoriteId();
        favoriteId.setUserId(userId);
        favoriteId.setStoryId(storyId);
        favorite.setId(favoriteId);
        favorite.setUser(user);
        favorite.setStory(story);

        Favorite savedFavorite = favoriteRepository.save(favorite);
        log.info("Đã thêm truyện vào yêu thích thành công: storyId={}, userId={}", storyId, userId);

        return favoriteMapper.toFavoriteResponse(savedFavorite);
    }

    @Override
    public void removeFromFavorite(Long storyId, Long userId) {
        log.info("Xóa truyện khỏi yêu thích: storyId={}, userId={}", storyId, userId);

        // Kiểm tra favorite tồn tại
        if (!favoriteRepository.existsByUserIdAndStoryId(userId, storyId)) {
            throw new ResourceNotFoundException("Không tìm thấy truyện trong danh sách yêu thích");
        }

        favoriteRepository.deleteByUserIdAndStoryId(userId, storyId);
        log.info("Đã xóa truyện khỏi yêu thích thành công: storyId={}, userId={}", storyId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public FavoriteListResponse getUserFavorites(Long userId, Pageable pageable) {
        log.info("Lấy danh sách yêu thích của user: userId={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        // Kiểm tra user tồn tại
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy user với ID: " + userId);
        }

        Page<Favorite> favoritesPage = favoriteRepository.findByUserIdWithFetch(userId, pageable);

        FavoriteListResponse response = new FavoriteListResponse();
        response.setContent(favoritesPage.getContent().stream()
                .map(favoriteMapper::toFavoriteResponse)
                .toList());
        response.setPage(favoritesPage.getNumber());
        response.setSize(favoritesPage.getSize());
        response.setTotalElements(favoritesPage.getTotalElements());
        response.setTotalPages(favoritesPage.getTotalPages());
        response.setHasNext(favoritesPage.hasNext());
        response.setHasPrevious(favoritesPage.hasPrevious());

        log.info("Đã lấy danh sách yêu thích thành công: userId={}, total={}",
                userId, favoritesPage.getTotalElements());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserFavoriteStory(Long storyId, Long userId) {
        return favoriteRepository.isUserFavoriteStory(userId, storyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countUserFavorites(Long userId) {
        return favoriteRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countStoryFavorites(Long storyId) {
        return favoriteRepository.countByStoryId(storyId);
    }

}
