package org.app.mintonmatchapi.notification.service;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.notification.dto.NotificationResponse;
import org.app.mintonmatchapi.notification.entity.Notification;
import org.app.mintonmatchapi.notification.event.NotificationDispatchCommand;
import org.app.mintonmatchapi.notification.repository.NotificationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    public static final int TITLE_MAX = 200;

    private final ApplicationEventPublisher eventPublisher;
    private final NotificationRepository notificationRepository;

    public NotificationService(ApplicationEventPublisher eventPublisher,
                              NotificationRepository notificationRepository) {
        this.eventPublisher = eventPublisher;
        this.notificationRepository = notificationRepository;
    }

    /**
     * 호출한 트랜잭션이 성공적으로 커밋된 뒤 알림을 저장한다.
     */
    public void publishAfterCommit(NotificationDispatchCommand command) {
        eventPublisher.publishEvent(command);
    }

    public static String truncateTitle(String title) {
        if (title == null) {
            return "";
        }
        if (title.length() <= TITLE_MAX) {
            return title;
        }
        return title.substring(0, TITLE_MAX - 1) + "…";
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUser_IdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (n.getReadAt() == null) {
            n.markRead(LocalDateTime.now());
        }
        return NotificationResponse.from(n);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllReadByUserId(userId, LocalDateTime.now());
    }
}
