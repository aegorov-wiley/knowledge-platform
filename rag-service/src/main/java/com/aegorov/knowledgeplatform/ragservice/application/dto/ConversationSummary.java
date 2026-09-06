package com.aegorov.knowledgeplatform.ragservice.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummary(
        UUID conversationId,
        Instant lastActivity,
        String preview
) {
}
