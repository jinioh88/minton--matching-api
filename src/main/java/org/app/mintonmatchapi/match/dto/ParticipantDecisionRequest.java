package org.app.mintonmatchapi.match.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParticipantDecisionRequest {

    @NotNull(message = "수락/거절 액션을 선택해 주세요.")
    private ParticipantDecisionAction action;

    public enum ParticipantDecisionAction {
        ACCEPT,
        REJECT
    }
}
