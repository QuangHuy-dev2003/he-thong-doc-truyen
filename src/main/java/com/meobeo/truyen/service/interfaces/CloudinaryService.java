package com.meobeo.truyen.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    /**
     * Upload ảnh lên Cloudinary
     */
    String uploadImage(MultipartFile file, String folder);

    /**
     * Xóa ảnh từ Cloudinary
     */
    void deleteImage(String publicId);

    /**
     * Validate file ảnh
     */
    boolean isValidImageFile(MultipartFile file);

    /**
     * Lấy public ID từ URL
     */
    String getPublicIdFromUrl(String url);
}