package org.app.mintonmatchapi.penalty.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.app.mintonmatchapi.common.entity.BaseEntity;
import org.app.mintonmatchapi.match.entity.Match;
import org.app.mintonmatchapi.user.entity.User;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "penalties",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_penalty_match_user_type",
                columnNames = {"match_id", "penalized_user_id", "type"}
        ),
        indexes = @Index(name = "idx_penalties_penalized_created", columnList = "penalized_user_id, created_at")
)
public class Penalty extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "penalty_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "penalized_user_id", nullable = false)
    private User penalizedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PenaltyType type;

    @Builder
    public Penalty(Match match, User host, User penalizedUser, PenaltyType type) {
        this.match = match;
        this.host = host;
        this.penalizedUser = penalizedUser;
        this.type = type;
    }
}
