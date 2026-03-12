package org.app.mintonmatchapi.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.config.S3Properties;
import org.app.mintonmatchapi.file.dto.FileUploadType;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Pattern S3_URL_PATTERN = Pattern.compile("https://[^/]+\\.s3\\.[^/]+\\.amazonaws\\.com/(.+)");

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    /**
     * 이미지 파일을 S3에 업로드하고 공개 URL을 반환합니다.
     *
     * @param inputStream   파일 입력 스트림
     * @param contentType  Content-Type (image/jpeg, image/png 등)
     * @param originalName 원본 파일명 (확장자 추출용)
     * @param type         업로드 용도 (PROFILE, MATCH 등, null 가능)
     * @param userId       사용자 ID (프로필 이미지 시 사용, null 가능)
     * @return 업로드된 파일의 URL과 S3 키
     */
    public FileUploadResult uploadImage(InputStream inputStream, String contentType, String originalName,
                                        FileUploadType type, Long userId) throws IOException {
        validateImage(contentType, originalName);

        String extension = extractExtension(originalName, contentType);
        String key = buildKey(type, userId, extension);

        String bucket = s3Properties.getBucketName();
        byte[] bytes = inputStream.readAllBytes();

        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength((long) bytes.length)
                        .build(),
                RequestBody.fromBytes(bytes));

        String url = buildPublicUrl(bucket, key);

        return new FileUploadResult(url, key);
    }

    /**
     * S3 객체를 삭제합니다.
     * 기존 프로필 이미지 교체 시 호출.
     *
     * @param key S3 객체 키
     */
    public void deleteObject(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        String bucket = s3Properties.getBucketName();
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    /**
     * S3 URL로 객체를 삭제합니다.
     * 본 서비스 S3 버킷 URL인 경우에만 삭제 수행.
     *
     * @param url S3 객체의 공개 URL
     */
    public void deleteObjectByUrl(String url) {
        String key = extractKeyFromUrl(url);
        if (key != null) {
            deleteObject(key);
        }
    }

    /**
     * S3 URL에서 객체 키를 추출합니다.
     * 본 서비스 S3 버킷 URL인 경우에만 키 추출.
     */
    public String extractKeyFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String bucket = s3Properties.getBucketName();
        String expectedPrefix = String.format("https://%s.s3.", bucket);
        if (!url.startsWith(expectedPrefix)) {
            return null;
        }
        Matcher matcher = S3_URL_PATTERN.matcher(url);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    private void validateImage(String contentType, String originalName) {
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "허용된 이미지 형식만 업로드 가능합니다. (jpeg, png, gif, webp)");
        }
        String ext = extractExtension(originalName, contentType);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "허용된 이미지 확장자만 업로드 가능합니다. (jpg, jpeg, png, gif, webp)");
        }
    }

    private String extractExtension(String originalName, String contentType) {
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
            if (ALLOWED_EXTENSIONS.contains(ext)) {
                return ext;
            }
        }
        return contentTypeToExtension(contentType);
    }

    private String contentTypeToExtension(String contentType) {
        return switch (contentType != null ? contentType.toLowerCase() : "") {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private String buildKey(FileUploadType type, Long userId, String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String prefix = type != null ? type.name().toLowerCase() : "uploads";
        if (type == FileUploadType.PROFILE && userId != null) {
            return String.format("%s/%d/%s.%s", prefix, userId, uuid, extension);
        }
        return String.format("%s/%s.%s", prefix, uuid, extension);
    }

    private String buildPublicUrl(String bucket, String key) {
        String region = s3Properties.getRegion();
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    public record FileUploadResult(String url, String key) {}
}
