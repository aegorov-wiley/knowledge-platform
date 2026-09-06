package com.aegorov.knowledgeplatform.ragservice.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationMessage(
        UUID id,
        String role,
        String content,
        Instant timestamp
) {
}
