package org.app.mintonmatchapi.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.app.mintonmatchapi.chat.entity.ChatMessageType;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageSendRequest {

    @NotBlank
    @Size(max = 1000)
    private String content;

    private ChatMessageType messageType;
}
