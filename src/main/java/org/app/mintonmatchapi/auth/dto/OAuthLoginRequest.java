package org.app.mintonmatchapi.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.app.mintonmatchapi.user.entity.Provider;
import org.springframework.util.StringUtils;

@Getter
@Setter
@NoArgsConstructor
public class OAuthLoginRequest {

    @NotNull(message = "provider는 필수입니다.")
    private Provider provider;

    private String socialAccessToken;

    private String authorizationCode;

    private String redirectUri;

    @jakarta.validation.constraints.AssertTrue(message = "socialAccessToken 또는 authorizationCode+redirectUri 조합이 필요합니다.")
    public boolean isValidLoginPayload() {
        if (StringUtils.hasText(socialAccessToken)) {
            return true;
        }
        return StringUtils.hasText(authorizationCode) && StringUtils.hasText(redirectUri);
    }
}
