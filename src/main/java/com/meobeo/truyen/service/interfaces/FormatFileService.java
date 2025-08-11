package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.story.FormatFileRequest;
import com.meobeo.truyen.domain.response.story.FormatFileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface FormatFileService {

    /**
     * Bắt đầu format file truyện bất đồng bộ
     * 
     * @param txtFile File TXT cần format
     * @param request Thông tin format
     * @param userId  ID người dùng
     * @return Job ID để track progress
     */
    String startFormatFile(MultipartFile txtFile, FormatFileRequest request, Long userId);

    /**
     * Lấy trạng thái format job
     * 
     * @param jobId Job ID
     * @return Thông tin progress
     */
    Optional<FormatFileResponse> getFormatStatus(String jobId);

    /**
     * Lấy danh sách job format của user
     * 
     * @param userId ID người dùng
     * @return Danh sách job
     */
    List<FormatFileResponse> getUserFormatJobs(Long userId);

    /**
     * Hủy format job
     * 
     * @param jobId  Job ID
     * @param userId ID người dùng
     * @return true nếu hủy thành công
     */
    boolean cancelFormat(String jobId, Long userId);

    /**
     * Xóa job đã hoàn thành (cleanup)
     * 
     * @param jobId Job ID
     */
    void cleanupCompletedFormatJob(String jobId);

    /**
     * Download file đã format
     * 
     * @param jobId  Job ID
     * @param userId ID người dùng
     * @return Byte array của file
     */
    byte[] downloadFormattedFile(String jobId, Long userId);
}
