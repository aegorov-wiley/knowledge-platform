package com.aegorov.knowledgeplatform.documentservice.application.dto;

import com.aegorov.knowledgeplatform.documentservice.persistence.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentUploadResponse(
        UUID id,
        String fileName,
        DocumentStatus status,
        Instant createdAt
) {
}
