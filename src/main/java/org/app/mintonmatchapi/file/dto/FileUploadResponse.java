package org.app.mintonmatchapi.file.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileUploadResponse {

    /**
     * S3에 저장된 파일의 공개 접근 URL
     */
    private final String url;

    /**
     * S3 객체 키 (삭제 시 사용)
     */
    private final String key;
}
