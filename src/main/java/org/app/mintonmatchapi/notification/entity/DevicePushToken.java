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
        name = "device_push_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_push_tokens_token", columnNames = "token")
)
public class DevicePushToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Column(name = "platform", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PushPlatform platform;

    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    @Builder
    public DevicePushToken(User user, String token, PushPlatform platform) {
        this.user = user;
        this.token = token;
        this.platform = platform;
    }

    public void disable() {
        this.disabledAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return disabledAt == null;
    }

    /** 동일·타 계정으로 토큰 소유를 옮기거나, 비활성 행을 다시 켠다. */
    public void assignActive(User newUser, PushPlatform newPlatform) {
        this.user = newUser;
        this.platform = newPlatform;
        this.disabledAt = null;
    }
}
