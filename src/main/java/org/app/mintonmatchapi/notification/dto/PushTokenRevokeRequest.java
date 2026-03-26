package org.app.mintonmatchapi.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PushTokenRevokeRequest {

    @NotBlank
    @Size(max = 512)
    private String token;
}
