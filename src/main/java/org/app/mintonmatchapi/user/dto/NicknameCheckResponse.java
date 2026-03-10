package org.app.mintonmatchapi.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NicknameCheckResponse {

    private boolean available;
}
