package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.story.TxtImportRequest;
import com.meobeo.truyen.domain.response.story.TxtImportResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface TxtImportService {

    /**
     * Bắt đầu import TXT file bất đồng bộ
     * 
     * @param txtFile File TXT
     * @param request Thông tin import
     * @param userId  ID người dùng
     * @return Job ID để track progress
     */
    String startTxtImport(MultipartFile txtFile, TxtImportRequest request, Long userId);

    /**
     * Lấy trạng thái import job
     * 
     * @param jobId Job ID
     * @return Thông tin progress
     */
    Optional<TxtImportResponse> getImportStatus(String jobId);

    /**
     * Hủy import job
     * 
     * @param jobId  Job ID
     * @param userId ID người dùng
     * @return true nếu hủy thành công
     */
    boolean cancelImport(String jobId, Long userId);

    /**
     * Xóa job đã hoàn thành (cleanup)
     * 
     * @param jobId Job ID
     */
    void cleanupCompletedJob(String jobId);
}
