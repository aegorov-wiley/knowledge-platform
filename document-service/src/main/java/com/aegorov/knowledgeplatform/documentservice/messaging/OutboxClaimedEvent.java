package com.aegorov.knowledgeplatform.documentservice.messaging;

import java.util.UUID;

public record OutboxClaimedEvent(
        UUID id,
        UUID aggregateId,
        String eventType,
        String payload
) {
}
