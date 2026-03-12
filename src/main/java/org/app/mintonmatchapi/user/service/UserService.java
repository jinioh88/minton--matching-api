package org.app.mintonmatchapi.user.service;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.file.dto.FileUploadType;
import org.app.mintonmatchapi.file.service.S3Service;
import org.app.mintonmatchapi.user.dto.NicknameCheckResponse;
import org.app.mintonmatchapi.user.dto.ProfileResponse;
import org.app.mintonmatchapi.user.dto.ProfileUpdateRequest;
import org.app.mintonmatchapi.user.entity.User;
import org.app.mintonmatchapi.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final S3Service s3Service;

    public UserService(UserRepository userRepository, S3Service s3Service) {
        this.userRepository = userRepository;
        this.s3Service = s3Service;
    }

    public NicknameCheckResponse checkNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return NicknameCheckResponse.builder().available(false).build();
        }
        boolean available = !userRepository.existsByNickname(nickname.trim());
        return NicknameCheckResponse.builder()
                .available(available)
                .build();
    }

    public ProfileResponse getMyProfile(Long userId) {
        User user = findUserById(userId);
        return ProfileResponse.ofMe(user);
    }

    @Transactional
    public ProfileResponse updateMyProfile(Long userId, ProfileUpdateRequest request) {
        User user = findUserById(userId);

        validateProfileUpdate(user, request);

        user.updateProfile(
                request.getNickname(),
                request.getProfileImg(),
                request.getLevel(),
                request.getInterestLoc1(),
                request.getInterestLoc2(),
                request.getRacketInfo(),
                request.getPlayStyle()
        );

        return ProfileResponse.ofMe(user);
    }

    @Transactional
    public ProfileResponse uploadProfileImage(Long userId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "업로드할 이미지가 없습니다.");
        }

        User user = findUserById(userId);

        // 기존 프로필 이미지가 본 서비스 S3 버킷 경로인 경우 삭제
        String existingProfileImg = user.getProfileImg();
        if (existingProfileImg != null && !existingProfileImg.isBlank()) {
            s3Service.deleteObjectByUrl(existingProfileImg);
        }

        // S3 업로드 → URL 획득 → User.profileImg 업데이트
        try {
            S3Service.FileUploadResult result = s3Service.uploadImage(
                    image.getInputStream(),
                    image.getContentType(),
                    image.getOriginalFilename(),
                    FileUploadType.PROFILE,
                    userId
            );
            user.updateProfile(null, result.url(), null, null, null, null, null);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "프로필 이미지 업로드 중 오류가 발생했습니다.");
        }

        return ProfileResponse.ofMe(user);
    }

    public ProfileResponse getUserProfile(Long userId) {
        User user = findUserById(userId);
        return ProfileResponse.ofOther(user);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private void validateProfileUpdate(User user, ProfileUpdateRequest request) {
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            if (userRepository.existsByNickname(request.getNickname()) && !request.getNickname().equals(user.getNickname())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "이미 사용 중인 닉네임입니다.");
            }
        }

        // 신규 가입 시 필수 프로필(nickname, interestLoc1) 검증
        boolean needsNickname = (user.getNickname() == null || user.getNickname().isBlank());
        boolean needsInterestLoc1 = (user.getInterestLoc1() == null || user.getInterestLoc1().isBlank());
        boolean hasNickname = request.getNickname() != null && !request.getNickname().isBlank();
        boolean hasInterestLoc1 = request.getInterestLoc1() != null && !request.getInterestLoc1().isBlank();

        if (needsNickname && !hasNickname) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "닉네임은 필수입니다.");
        }
        if (needsInterestLoc1 && !hasInterestLoc1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "관심 지역 1은 필수입니다.");
        }
    }
}
