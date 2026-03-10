package org.app.mintonmatchapi.auth.dto;

import lombok.Builder;
import lombok.Getter;
import org.app.mintonmatchapi.user.entity.User;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private UserResponse user;

    @Getter
    @Builder
    public static class UserResponse {
        private Long id;
        private String email;
        private String nickname;
        private String profileImg;
        private boolean profileComplete; // 닉네임, interestLoc1 등 필수 프로필 입력 여부

        public static UserResponse from(User user) {
            boolean profileComplete = user.getNickname() != null && !user.getNickname().isBlank()
                    && user.getInterestLoc1() != null && !user.getInterestLoc1().isBlank();

            return UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .profileImg(user.getProfileImg())
                    .profileComplete(profileComplete)
                    .build();
        }
    }
}
