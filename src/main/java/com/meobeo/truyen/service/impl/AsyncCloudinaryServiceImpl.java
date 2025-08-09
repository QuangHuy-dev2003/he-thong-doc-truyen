package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.Story;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.repository.StoryRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.AsyncCloudinaryService;
import com.meobeo.truyen.service.interfaces.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncCloudinaryServiceImpl implements AsyncCloudinaryService {

    private final CloudinaryService cloudinaryService;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;

    @Override
    @Async("taskExecutor")
    public void uploadStoryCoverImageAsync(Long storyId, MultipartFile coverImage) {
        try {
            log.info("Bắt đầu upload ảnh bìa bất đồng bộ cho truyện: storyId={}", storyId);

            // Upload lên Cloudinary
            String coverImageUrl = cloudinaryService.uploadImage(coverImage, "story-covers");

            // Cập nhật URL vào database
            Story story = storyRepository.findById(storyId).orElse(null);
            if (story != null) {
                story.setCoverImageUrl(coverImageUrl);
                storyRepository.save(story);
                log.info("Upload ảnh bìa thành công: storyId={}, url={}", storyId, coverImageUrl);
            } else {
                log.warn("Không tìm thấy truyện để cập nhật ảnh bìa: storyId={}", storyId);
            }

        } catch (Exception e) {
            log.error("Lỗi upload ảnh bìa bất đồng bộ: storyId={}, error={}", storyId, e.getMessage(), e);
            // Không throw exception vì đây là async method
        }
    }

    @Override
    @Async("taskExecutor")
    public void uploadUserAvatarAsync(Long userId, MultipartFile avatarImage) {
        try {
            log.info("Bắt đầu upload avatar bất đồng bộ cho user: userId={}", userId);

            // Upload lên Cloudinary
            String avatarUrl = cloudinaryService.uploadImage(avatarImage, "user-avatars");

            // Cập nhật URL vào database
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setAvatarUrl(avatarUrl);
                userRepository.save(user);
                log.info("Upload avatar thành công: userId={}, url={}", userId, avatarUrl);
            } else {
                log.warn("Không tìm thấy user để cập nhật avatar: userId={}", userId);
            }

        } catch (Exception e) {
            log.error("Lỗi upload avatar bất đồng bộ: userId={}, error={}", userId, e.getMessage(), e);
            // Không throw exception vì đây là async method
        }
    }

    @Override
    @Async("taskExecutor")
    public void deleteImageAsync(String publicId) {
        try {
            log.info("Bắt đầu xóa ảnh bất đồng bộ: publicId={}", publicId);

            cloudinaryService.deleteImage(publicId);

            log.info("Xóa ảnh thành công: publicId={}", publicId);

        } catch (Exception e) {
            log.error("Lỗi xóa ảnh bất đồng bộ: publicId={}, error={}", publicId, e.getMessage(), e);
            // Không throw exception vì đây là async method
        }
    }
}