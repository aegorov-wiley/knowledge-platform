package com.aegorov.knowledgeplatform.indexingservice.messaging;

import com.aegorov.knowledgeplatform.indexingservice.application.DocumentIndexingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentUploadedListener {

    private final DocumentIndexingService documentIndexingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${knowledge-platform.kafka.topics.documents-uploaded}",
            groupId = "${knowledge-platform.kafka.consumer.group-id}"
    )
    public void onDocumentUploaded(String payload, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        DocumentUploadedV1 event = deserialize(payload);
        log.debug("Received document upload event: key={}, eventId={}", key, event.eventId());
        documentIndexingService.index(event);
    }

    private DocumentUploadedV1 deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, DocumentUploadedV1.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize DocumentUploadedV1 payload", ex);
        }
    }
}
