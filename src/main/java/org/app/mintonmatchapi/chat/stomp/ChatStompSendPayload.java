package org.app.mintonmatchapi.chat.stomp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.app.mintonmatchapi.chat.entity.ChatMessageType;

@Getter
@Setter
@NoArgsConstructor
public class ChatStompSendPayload {

    @NotNull
    private Long roomId;

    @NotBlank
    @Size(max = 1000)
    private String content;

    private ChatMessageType messageType;
}
