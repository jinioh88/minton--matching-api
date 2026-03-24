package org.app.mintonmatchapi.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.app.mintonmatchapi.common.entity.BaseEntity;
import org.app.mintonmatchapi.match.entity.Match;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(name = "uk_chat_rooms_match_id", columnNames = "match_id")
)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Builder
    public ChatRoom(Match match) {
        this.match = match;
    }
}
