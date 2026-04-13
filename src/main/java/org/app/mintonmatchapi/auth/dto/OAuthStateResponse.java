package org.app.mintonmatchapi.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthStateResponse {

    private final String state;
    private final long expiresInSeconds;
}
