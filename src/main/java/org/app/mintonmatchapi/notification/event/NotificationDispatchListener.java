package org.app.mintonmatchapi.notification.event;

import lombok.extern.slf4j.Slf4j;
import org.app.mintonmatchapi.notification.dto.NotificationRealtimePayload;
import org.app.mintonmatchapi.notification.entity.Notification;
import org.app.mintonmatchapi.notification.repository.NotificationRepository;
import org.app.mintonmatchapi.notification.service.NotificationOutboundService;
import org.app.mintonmatchapi.user.entity.User;
import org.app.mintonmatchapi.user.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 참여·매칭 트랜잭션 커밋 이후 알림을 별도 트랜잭션으로 저장한다.
 */
@Slf4j
@Component
public class NotificationDispatchListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationOutboundService notificationOutboundService;

    public NotificationDispatchListener(NotificationRepository notificationRepository,
                                       UserRepository userRepository,
                                       NotificationOutboundService notificationOutboundService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationOutboundService = notificationOutboundService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDispatch(NotificationDispatchCommand command) {
        try {
            User user = userRepository.findById(command.recipientUserId()).orElse(null);
            if (user == null) {
                log.warn("Notification skipped: user not found userId={}", command.recipientUserId());
                return;
            }
            Notification notification = Notification.builder()
                    .user(user)
                    .type(command.type())
                    .title(command.title())
                    .body(command.body())
                    .payload(command.payload())
                    .relatedMatchId(command.relatedMatchId())
                    .relatedParticipantId(command.relatedParticipantId())
                    .build();
            Notification saved = notificationRepository.save(notification);
            notificationOutboundService.dispatchAfterPersist(NotificationRealtimePayload.from(saved));
        } catch (Exception e) {
            log.warn("Failed to persist notification type={} recipientUserId={}",
                    command.type(), command.recipientUserId(), e);
        }
    }
}
