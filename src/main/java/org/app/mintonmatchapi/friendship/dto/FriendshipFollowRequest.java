package org.app.mintonmatchapi.friendship.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FriendshipFollowRequest {

    @NotNull
    private Long followingUserId;
}
