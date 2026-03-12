package org.app.mintonmatchapi.match.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.app.mintonmatchapi.user.entity.User;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "match_participants")
public class MatchParticipant {

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

    @Column(name = "attendance", length = 20)
    @Enumerated(EnumType.STRING)
    private Attendance attendance = Attendance.UNDECIDED;

    @Builder
    public MatchParticipant(Match match, User user, ParticipantStatus status, Integer queueOrder,
                            String applyMessage, Attendance attendance) {
        this.match = match;
        this.user = user;
        this.status = status;
        this.queueOrder = queueOrder != null ? queueOrder : 0;
        this.applyMessage = applyMessage;
        this.attendance = attendance != null ? attendance : Attendance.UNDECIDED;
    }
}
