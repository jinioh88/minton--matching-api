package org.app.mintonmatchapi.notification.service;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.notification.entity.DevicePushToken;
import org.app.mintonmatchapi.notification.entity.PushPlatform;
import org.app.mintonmatchapi.notification.repository.DevicePushTokenRepository;
import org.app.mintonmatchapi.user.entity.User;
import org.app.mintonmatchapi.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushTokenService {

    private final DevicePushTokenRepository devicePushTokenRepository;
    private final UserRepository userRepository;

    public PushTokenService(DevicePushTokenRepository devicePushTokenRepository,
                           UserRepository userRepository) {
        this.devicePushTokenRepository = devicePushTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * FCM 토큰 등록·갱신. 동일 토큰이 다른 사용자에 묶여 있으면 현재 사용자로 이전한다.
     */
    @Transactional
    public void registerOrUpdate(Long userId, String fcmToken, PushPlatform platform) {
        String token = fcmToken.trim();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        DevicePushToken row = devicePushTokenRepository.findByToken(token).orElse(null);
        if (row == null) {
            devicePushTokenRepository.save(DevicePushToken.builder()
                    .user(user)
                    .token(token)
                    .platform(platform)
                    .build());
            return;
        }
        row.assignActive(user, platform);
    }

    /**
     * 로그아웃 등 — 본인 소유 토큰만 비활성화. 없으면 무시(멱등).
     */
    @Transactional
    public void revoke(Long userId, String rawToken) {
        String token = rawToken.trim();
        devicePushTokenRepository.findByToken(token).ifPresent(row -> {
            if (row.getUser().getId().equals(userId) && row.isActive()) {
                row.disable();
            }
        });
    }

    @Transactional
    public void deactivateByToken(String rawToken) {
        String token = rawToken.trim();
        devicePushTokenRepository.findByToken(token).ifPresent(row -> {
            if (row.isActive()) {
                row.disable();
            }
        });
    }
}
