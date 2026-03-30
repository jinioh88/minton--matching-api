package org.app.mintonmatchapi.friendship.service;

import org.app.mintonmatchapi.common.exception.BusinessException;
import org.app.mintonmatchapi.common.exception.ErrorCode;
import org.app.mintonmatchapi.friendship.dto.FollowingUserResponse;
import org.app.mintonmatchapi.friendship.entity.Friendship;
import org.app.mintonmatchapi.friendship.repository.FriendshipRepository;
import org.app.mintonmatchapi.user.entity.User;
import org.app.mintonmatchapi.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FollowingUserResponse follow(Long followerUserId, Long followingUserId) {
        if (followerUserId.equals(followingUserId)) {
            throw new BusinessException(ErrorCode.FRIENDSHIP_SELF_NOT_ALLOWED);
        }
        if (friendshipRepository.existsByFollower_IdAndFollowing_Id(followerUserId, followingUserId)) {
            throw new BusinessException(ErrorCode.FRIENDSHIP_ALREADY_EXISTS);
        }
        User follower = userRepository.findById(followerUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User following = userRepository.findById(followingUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Friendship saved = friendshipRepository.save(Friendship.builder()
                .follower(follower)
                .following(following)
                .build());
        return FollowingUserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FollowingUserResponse> listFollowing(Long followerUserId) {
        return friendshipRepository.findAllByFollower_IdWithFollowingOrderByCreatedAtDesc(followerUserId).stream()
                .map(FollowingUserResponse::from)
                .toList();
    }

    /**
     * 친구 활동 알림 수신자 ID — {@code following_id = activeUserId} 인 모든 {@code follower_id}.
     * 활동 주체 본인은 제외(스키마상 불가능하더라도 방어). 비어 있으면 호출부에서 {@code publishAfterCommit} 등을 호출하지 않는다.
     */
    @Transactional(readOnly = true)
    public List<Long> findFollowerIdsForFriendActivityNotification(Long activeUserId) {
        List<Long> followerIds = friendshipRepository.findFollowerIdsByFollowing_Id(activeUserId);
        if (followerIds.isEmpty()) {
            return List.of();
        }
        return followerIds.stream()
                .filter(id -> !id.equals(activeUserId))
                .distinct()
                .toList();
    }

    @Transactional
    public void unfollow(Long followerUserId, Long followingUserId) {
        Friendship row = friendshipRepository
                .findByFollower_IdAndFollowing_Id(followerUserId, followingUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIENDSHIP_NOT_FOUND));
        friendshipRepository.delete(row);
    }
}
