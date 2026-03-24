package org.app.mintonmatchapi.chat.entity;

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
        name = "chat_messages",
        indexes = @Index(name = "idx_chat_messages_room_message", columnList = "room_id, message_id")
)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "message_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ChatMessageType messageType;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public ChatMessage(ChatRoom room, User sender, String content, ChatMessageType messageType) {
        this.room = room;
        this.sender = sender;
        this.content = content;
        this.messageType = messageType != null ? messageType : ChatMessageType.TEXT;
    }

    public void markEdited(String newContent) {
        this.content = newContent;
        this.editedAt = LocalDateTime.now();
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }
}
