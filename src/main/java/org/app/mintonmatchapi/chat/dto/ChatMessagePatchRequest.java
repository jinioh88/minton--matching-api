package org.app.mintonmatchapi.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessagePatchRequest {

    @NotBlank
    @Size(max = 1000)
    private String content;
}
