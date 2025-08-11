package com.meobeo.truyen.domain.request.story;

import lombok.Data;

/**
 * Request DTO cho API format file truyện
 */
@Data
public class FormatFileRequest {

    /**
     * Tên file gốc (để tạo tên file output)
     */
    private String originalFileName;

    /**
     * Có loại bỏ watermark hay không
     */
    private Boolean removeWatermark = true;

    /**
     * Có loại bỏ ký tự đặc biệt ở đầu dòng hay không
     */
    private Boolean removeSpecialChars = true;

    /**
     * Có gộp nhiều dòng trống thành 1 dòng hay không
     */
    private Boolean mergeEmptyLines = true;

    /**
     * Có format dấu câu hay không
     */
    private Boolean formatPunctuation = true;
}
