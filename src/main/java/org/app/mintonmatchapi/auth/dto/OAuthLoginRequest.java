package org.app.mintonmatchapi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.app.mintonmatchapi.user.entity.Provider;

@Getter
@Setter
@NoArgsConstructor
public class OAuthLoginRequest {

    @NotNull(message = "provider는 필수입니다.")
    private Provider provider;

    @NotBlank(message = "authorizationCode는 필수입니다.")
    private String authorizationCode;

    @NotBlank(message = "redirectUri는 필수입니다.")
    private String redirectUri;
}
