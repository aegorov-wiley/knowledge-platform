package com.aegorov.knowledgeplatform.documentservice.application.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
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
