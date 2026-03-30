package org.app.mintonmatchapi.friendship.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.friendship.entity.Friendship;
import org.app.mintonmatchapi.user.entity.Level;
import org.app.mintonmatchapi.user.entity.User;

import java.time.LocalDateTime;

/**
 * 내 팔로잉 목록 항목 — 프로필 카드에 쓰기 좋은 최소 필드 + 팔로우 시각.
 */
@Getter
@Builder
public class FollowingUserResponse {

    private Long userId;
    private String nickname;
    private String profileImg;
    private Level level;
    private LocalDateTime followedAt;

    public static FollowingUserResponse from(Friendship friendship) {
        User u = friendship.getFollowing();
        return FollowingUserResponse.builder()
                .userId(u.getId())
                .nickname(u.getNickname())
                .profileImg(u.getProfileImg())
                .level(u.getLevel())
                .followedAt(friendship.getCreatedAt())
                .build();
    }
}
