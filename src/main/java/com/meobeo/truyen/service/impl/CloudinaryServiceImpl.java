package com.meobeo.truyen.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.meobeo.truyen.service.interfaces.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.max-file-size:5242880}") // 5MB default
    private long maxFileSize;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp");

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        try {
            // Validate file
            if (!isValidImageFile(file)) {
                throw new IllegalArgumentException("File không hợp lệ hoặc quá lớn");
            }

            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "transformation", "w_800,h_600,c_fill,q_auto"));

            String imageUrl = (String) uploadResult.get("secure_url");
            log.info("Upload ảnh thành công: {}", imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("Lỗi upload ảnh: {}", e.getMessage());
            throw new RuntimeException("Không thể upload ảnh", e);
        }
    }

    @Override
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Xóa ảnh thành công: {}", publicId);
        } catch (IOException e) {
            log.error("Lỗi xóa ảnh: {}", e.getMessage());
            throw new RuntimeException("Không thể xóa ảnh", e);
        }
    }

    @Override
    public boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        // Kiểm tra kích thước file
        if (file.getSize() > maxFileSize) {
            log.warn("File quá lớn: {} bytes", file.getSize());
            return false;
        }

        // Kiểm tra loại file
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Loại file không được hỗ trợ: {}", contentType);
            return false;
        }

        return true;
    }

    @Override
    public String getPublicIdFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            // Extract public ID from Cloudinary URL
            // Example:
            // https://res.cloudinary.com/cloud_name/image/upload/v1234567890/folder/image.jpg
            String[] parts = url.split("/upload/");
            if (parts.length < 2) {
                return null;
            }

            String afterUpload = parts[1];
            // Remove version if present
            if (afterUpload.contains("/v")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/v") + 2);
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }

            // Remove file extension
            int lastDotIndex = afterUpload.lastIndexOf(".");
            if (lastDotIndex > 0) {
                afterUpload = afterUpload.substring(0, lastDotIndex);
            }

            return afterUpload;
        } catch (Exception e) {
            log.error("Lỗi extract public ID từ URL: {}", url, e);
            return null;
        }
    }
}