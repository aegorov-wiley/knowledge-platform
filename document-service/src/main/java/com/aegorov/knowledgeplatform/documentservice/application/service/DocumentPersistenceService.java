package com.aegorov.knowledgeplatform.documentservice.application.service;

import com.aegorov.knowledgeplatform.documentservice.application.dto.DocumentUploadResponse;
import com.aegorov.knowledgeplatform.documentservice.application.dto.DocumentUploadedV1;
import com.aegorov.knowledgeplatform.documentservice.persistence.DocumentStatus;
import com.aegorov.knowledgeplatform.documentservice.persistence.DocumentEntity;
import com.aegorov.knowledgeplatform.documentservice.persistence.DocumentRepository;
import com.aegorov.knowledgeplatform.documentservice.persistence.OutboxEventEntity;
import com.aegorov.knowledgeplatform.documentservice.persistence.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DocumentPersistenceService {

    private static final String AGGREGATE_TYPE = "document";
    private static final String EVENT_TYPE = "DocumentUploadedV1";

    private final DocumentRepository documentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    @Retryable(
            retryFor = {
                    OptimisticLockException.class,
                    OptimisticLockingFailureException.class,
                    ObjectOptimisticLockingFailureException.class,
                    DataIntegrityViolationException.class
            },
            maxAttemptsExpression = "${knowledge-platform.retry.create-document.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${knowledge-platform.retry.create-document.delay-ms:100}",
                    maxDelayExpression = "${knowledge-platform.retry.create-document.max-delay-ms:1000}",
                    multiplierExpression = "${knowledge-platform.retry.create-document.multiplier:2.0}"
            )
    )
    public DocumentUploadResponse createDocument(UUID documentId, String fileName, String contentType, long size,
            String storageKey
    ) {
        Instant now = clock.instant();
        var document = documentRepository.findByFileName(fileName)
                .map(existing -> existing
                        .setContentType(contentType)
                        .setSize(size)
                        .setStatus(DocumentStatus.UPLOADED)
                        .setStorageKey(storageKey)
                        .setUpdatedAt(now))
                .orElseGet(() -> DocumentEntity.builder()
                        .id(documentId)
                        .fileName(fileName)
                        .contentType(contentType)
                        .size(size)
                        .status(DocumentStatus.UPLOADED)
                        .storageKey(storageKey)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

        var savedDocument = documentRepository.save(document);

        UUID eventId = UUID.randomUUID();
        var eventPayload = DocumentUploadedV1.builder()
                .eventId(eventId.toString())
                .documentId(savedDocument.getId())
                .fileName(fileName)
                .contentType(contentType)
                .size(size)
                .storageKey(storageKey)
                .occurredAt(now)
                .build();

        outboxEventRepository.save(new OutboxEventEntity(
                eventId,
                savedDocument.getId(),
                AGGREGATE_TYPE,
                EVENT_TYPE,
                serialize(eventPayload),
                now
        ));

        return new DocumentUploadResponse(
                savedDocument.getId(),
                savedDocument.getFileName(),
                savedDocument.getStatus(),
                savedDocument.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public UploadTarget resolveUploadTarget(String fileName) {
        return documentRepository.findByFileName(fileName)
                .map(document -> UploadTarget.builder()
                        .documentId(document.getId())
                        .storageKey(document.getStorageKey())
                        .build())
                .orElseGet(() -> UploadTarget.builder().documentId(UUID.randomUUID()).build());
    }

    private String serialize(DocumentUploadedV1 eventPayload) {
        try {
            return objectMapper.writeValueAsString(eventPayload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
    }

    @Builder
    public record UploadTarget(UUID documentId, String storageKey) {
    }
}
