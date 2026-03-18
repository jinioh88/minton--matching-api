package org.app.mintonmatchapi.match.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParticipantApplyRequest {

    @Size(max = 200, message = "참여 멘트는 200자 이내로 입력해 주세요.")
    private String applyMessage;
}
