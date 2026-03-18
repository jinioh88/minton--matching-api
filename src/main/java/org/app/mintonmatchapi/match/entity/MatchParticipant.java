package org.app.mintonmatchapi.match.entity;

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
@Table(name = "match_participants", indexes = {
        @Index(name = "idx_match_status_queue", columnList = "match_id, status, queue_order"),
        @Index(name = "idx_status_offer_expires", columnList = "status, offer_expires_at")
})
public class MatchParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    @Column(name = "queue_order", nullable = false)
    private Integer queueOrder = 0;

    @Column(name = "apply_message", length = 200)
    private String applyMessage;

    @Column(name = "offer_expires_at")
    private LocalDateTime offerExpiresAt;

    @Column(name = "attendance", length = 20)
    @Enumerated(EnumType.STRING)
    private Attendance attendance = Attendance.UNDECIDED;

    @Builder
    public MatchParticipant(Match match, User user, ParticipantStatus status, Integer queueOrder,
                            String applyMessage, LocalDateTime offerExpiresAt, Attendance attendance) {
        this.match = match;
        this.user = user;
        this.status = status;
        this.queueOrder = queueOrder != null ? queueOrder : 0;
        this.applyMessage = applyMessage;
        this.offerExpiresAt = offerExpiresAt;
        this.attendance = attendance != null ? attendance : Attendance.UNDECIDED;
    }

    public void changeToAccepted() {
        this.status = ParticipantStatus.ACCEPTED;
        this.queueOrder = 0;
    }

    public void changeToRejected() {
        this.status = ParticipantStatus.REJECTED;
    }

    public void changeToCancelled() {
        this.status = ParticipantStatus.CANCELLED;
    }

    public void changeToReserved(LocalDateTime offerExpiresAt) {
        this.status = ParticipantStatus.RESERVED;
        this.offerExpiresAt = offerExpiresAt;
    }

    public void changeToWaiting(int newQueueOrder) {
        this.status = ParticipantStatus.WAITING;
        this.queueOrder = newQueueOrder;
        this.offerExpiresAt = null;
    }

    /**
     * RESERVED 상태에서 예약 만료 여부
     */
    public boolean isOfferExpired() {
        if (status != ParticipantStatus.RESERVED || offerExpiresAt == null) {
            return false;
        }
        return offerExpiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 예약 수락 가능 상태인지 (RESERVED 또는 긴급 모드의 WAITING)
     * @param emergencyCutoff 긴급 모드 기준 시각 (예: 현재 + 2시간)
     */
    public boolean canAcceptOffer(LocalDateTime emergencyCutoff) {
        if (status == ParticipantStatus.RESERVED) {
            return true;
        }
        if (status == ParticipantStatus.WAITING && match.isWithinEmergencyThreshold(emergencyCutoff)) {
            return true;
        }
        return false;
    }
}
