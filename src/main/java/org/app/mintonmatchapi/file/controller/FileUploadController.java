package org.app.mintonmatchapi.file.controller;

import org.app.mintonmatchapi.auth.AuthUtils;
import org.app.mintonmatchapi.auth.UserPrincipal;
import org.app.mintonmatchapi.common.dto.ApiResponse;
import org.app.mintonmatchapi.file.dto.FileUploadResponse;
import org.app.mintonmatchapi.file.dto.FileUploadType;
import org.app.mintonmatchapi.file.service.FileUploadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * 이미지 파일 업로드 (Multipart/form-data)
     * - 허용: jpeg, png, gif, webp (최대 5MB)
     * - S3 업로드 후 공개 URL 반환
     *
     * @param file MultipartFile (file 파라미터)
     * @param type PROFILE, MATCH 등 (선택)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false) FileUploadType type,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = AuthUtils.getUserIdOrThrow(principal);
        FileUploadResponse response = fileUploadService.upload(file, type, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
