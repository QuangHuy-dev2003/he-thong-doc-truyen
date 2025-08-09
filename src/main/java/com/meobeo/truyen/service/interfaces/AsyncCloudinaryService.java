package com.meobeo.truyen.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface AsyncCloudinaryService {

    /**
     * Upload ảnh bìa truyện bất đồng bộ
     * 
     * @param storyId     ID của truyện
     * @param coverImage  File ảnh bìa
     */
    void uploadStoryCoverImageAsync(Long storyId, MultipartFile coverImage);

    /**
     * Upload ảnh avatar user bất đồng bộ
     * 
     * @param userId    ID của user
     * @param avatarImage File ảnh avatar
     */
    void uploadUserAvatarAsync(Long userId, MultipartFile avatarImage);

    /**
     * Xóa ảnh từ Cloudinary bất đồng bộ
     * 
     * @param publicId Public ID của ảnh trên Cloudinary
     */
    void deleteImageAsync(String publicId);
} 