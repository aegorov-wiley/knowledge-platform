package com.aegorov.knowledgeplatform.indexingservice.messaging;

import java.time.Instant;
import java.util.UUID;

public record DocumentUploadedV1(
        String eventId,
        UUID documentId,
        String fileName,
        String contentType,
        long size,
        String storageKey,
        Instant occurredAt
) {
}
