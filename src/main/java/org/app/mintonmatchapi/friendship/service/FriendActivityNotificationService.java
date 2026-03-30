package org.app.mintonmatchapi.friendship.service;

import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.match.entity.MatchParticipant;
import org.app.mintonmatchapi.notification.entity.NotificationType;
import org.app.mintonmatchapi.notification.event.NotificationDispatchCommand;
import org.app.mintonmatchapi.notification.service.NotificationService;
import org.app.mintonmatchapi.user.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 친구(팔로우) 관점 활동 알림 — {@link NotificationService#publishAfterCommit}만 사용한다.
 */
@Service
public class FriendActivityNotificationService {

    private final FriendshipService friendshipService;
    private final NotificationService notificationService;

    public FriendActivityNotificationService(FriendshipService friendshipService,
                                            NotificationService notificationService) {
        this.friendshipService = friendshipService;
        this.notificationService = notificationService;
    }

    public void publishNewMatchCreatedToFollowers(Match match, User host) {
        List<Long> recipientIds = friendshipService.findFollowerIdsForFriendActivityNotification(host.getId());
        if (recipientIds.isEmpty()) {
            return;
        }
        String nick = displayNickname(host);
        String title = NotificationService.truncateTitle("새 매칭");
        String body = String.format("%s님이 새로운 매칭을 만들었습니다! 지금 확인해보세요.", nick);
        Long matchId = match.getId();
        for (Long recipientId : recipientIds) {
            notificationService.publishAfterCommit(NotificationDispatchCommand.of(
                    recipientId,
                    NotificationType.FRIEND_CREATED_MATCH,
                    title,
                    body,
                    matchId,
                    null));
        }
    }

    public void publishParticipationConfirmedToFollowers(Match match, MatchParticipant participant) {
        User actor = participant.getUser();
        List<Long> recipientIds = friendshipService.findFollowerIdsForFriendActivityNotification(actor.getId());
        if (recipientIds.isEmpty()) {
            return;
        }
        String nick = displayNickname(actor);
        String title = NotificationService.truncateTitle("참여 확정");
        String body = String.format("%s님이 매칭 참여를 확정했습니다! 함께 참여하시겠어요?", nick);
        Long matchId = match.getId();
        Long participationId = participant.getId();
        for (Long recipientId : recipientIds) {
            notificationService.publishAfterCommit(NotificationDispatchCommand.of(
                    recipientId,
                    NotificationType.FRIEND_CONFIRMED_PARTICIPATION,
                    title,
                    body,
                    matchId,
                    participationId));
        }
    }

    private static String displayNickname(User user) {
        String n = user.getNickname();
        return (n != null && !n.isBlank()) ? n : "회원";
    }
}
