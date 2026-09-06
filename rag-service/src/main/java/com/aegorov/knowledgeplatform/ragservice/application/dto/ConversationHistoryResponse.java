package com.aegorov.knowledgeplatform.ragservice.application.dto;

import java.util.List;
import java.util.UUID;

public record ConversationHistoryResponse(
        UUID conversationId,
        List<ConversationMessage> messages
) {
}
