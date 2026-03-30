package org.app.mintonmatchapi.friendship.repository;

import org.app.mintonmatchapi.friendship.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    boolean existsByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    List<Friendship> findAllByFollower_Id(Long followerId);

    Optional<Friendship> findByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    @Query("SELECT f FROM Friendship f JOIN FETCH f.following WHERE f.follower.id = :followerId ORDER BY f.createdAt DESC")
    List<Friendship> findAllByFollower_IdWithFollowingOrderByCreatedAtDesc(@Param("followerId") Long followerId);

    @Query("SELECT f.follower.id FROM Friendship f WHERE f.following.id = :followingUserId")
    List<Long> findFollowerIdsByFollowing_Id(@Param("followingUserId") Long followingUserId);
}
