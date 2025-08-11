package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.story.TxtImportRequest;
import com.meobeo.truyen.domain.response.story.TxtImportResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public interface AsyncTxtImportService {

    /**
     * Xử lý file TXT bất đồng bộ
     * 
     * @param txtFile File TXT
     * @param request Thông tin import
     * @param userId  ID người dùng
     * @param jobId   Job ID để track progress
     */
    void processTxtFileAsync(MultipartFile txtFile, TxtImportRequest request, Long userId, String jobId);

    /**
     * Lấy map import jobs
     */
    Map<String, TxtImportResponse> getImportJobs();

    /**
     * Lấy map job user
     */
    Map<String, Long> getJobUserMap();

    /**
     * Lấy map cancel flags
     */
    Map<String, AtomicBoolean> getCancelFlags();

}
