package org.app.mintonmatchapi.file.service;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.file.dto.FileUploadResponse;
import org.app.mintonmatchapi.file.dto.FileUploadType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileUploadService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final S3Service s3Service;

    public FileUploadService(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    public FileUploadResponse upload(MultipartFile file, FileUploadType type, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "업로드할 파일이 없습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "파일 크기는 5MB 이하여야 합니다.");
        }

        try {
            S3Service.FileUploadResult result = s3Service.uploadImage(
                    file.getInputStream(),
                    file.getContentType(),
                    file.getOriginalFilename(),
                    type,
                    userId
            );
            return FileUploadResponse.builder()
                    .url(result.url())
                    .key(result.key())
                    .build();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 업로드 중 오류가 발생했습니다.");
        }
    }
}
