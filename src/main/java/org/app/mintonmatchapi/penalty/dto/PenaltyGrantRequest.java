package org.app.mintonmatchapi.penalty.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.app.mintonmatchapi.penalty.entity.PenaltyType;

@Getter
@Setter
public class PenaltyGrantRequest {

    @NotNull
    private Long userId;

    @NotNull
    private PenaltyType type;
}
