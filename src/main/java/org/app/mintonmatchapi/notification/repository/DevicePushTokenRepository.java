package org.app.mintonmatchapi.notification.repository;

import org.app.mintonmatchapi.notification.entity.DevicePushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DevicePushTokenRepository extends JpaRepository<DevicePushToken, Long> {

    Optional<DevicePushToken> findByToken(String token);

    List<DevicePushToken> findByUser_IdAndDisabledAtIsNull(Long userId);
}
