package org.app.mintonmatchapi.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.app.mintonmatchapi.common.entity.BaseEntity;
import org.app.mintonmatchapi.user.entity.User;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "notifications",
        indexes = @Index(name = "idx_notifications_user_created", columnList = "user_id, created_at")
)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /** JSON 문자열(딥링크·추가 필드). 클라이언트 파싱. */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "related_match_id")
    private Long relatedMatchId;

    @Column(name = "related_participant_id")
    private Long relatedParticipantId;

    @Builder
    public Notification(User user, NotificationType type, String title, String body, String payload,
                       Long relatedMatchId, Long relatedParticipantId) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.body = body;
        this.payload = payload;
        this.relatedMatchId = relatedMatchId;
        this.relatedParticipantId = relatedParticipantId;
    }

    public void markRead(LocalDateTime at) {
        this.readAt = at;
    }
}
