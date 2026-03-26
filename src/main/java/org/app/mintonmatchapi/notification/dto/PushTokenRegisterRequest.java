package org.app.mintonmatchapi.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.app.mintonmatchapi.notification.entity.PushPlatform;

@Getter
@Setter
@NoArgsConstructor
public class PushTokenRegisterRequest {

    @NotBlank
    @Size(max = 512)
    private String fcmToken;

    @NotNull
    private PushPlatform platform;
}
