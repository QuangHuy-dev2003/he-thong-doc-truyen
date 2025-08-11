package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.story.FormatFileRequest;
import com.meobeo.truyen.domain.response.story.FormatFileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public interface AsyncFormatFileService {

    /**
     * Xử lý format file bất đồng bộ
     * 
     * @param txtFile File TXT
     * @param request Thông tin format
     * @param userId  ID người dùng
     * @param jobId   Job ID
     */
    void processFormatFileAsync(MultipartFile txtFile, FormatFileRequest request, Long userId, String jobId);

    /**
     * Lấy map các job đang chạy
     */
    Map<String, FormatFileResponse> getFormatJobs();

    /**
     * Lấy map userId của từng job
     */
    Map<String, Long> getJobUserMap();

    /**
     * Lấy map cancel flag của từng job
     */
    Map<String, AtomicBoolean> getCancelFlags();
}
