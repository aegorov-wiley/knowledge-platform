package com.aegorov.knowledgeplatform.indexingservice.application;

import com.aegorov.knowledgeplatform.indexingservice.messaging.DocumentUploadedV1;
import com.aegorov.knowledgeplatform.indexingservice.persistence.DocumentChunkRepository;
import com.aegorov.knowledgeplatform.indexingservice.persistence.DocumentChunkWriter;
import com.aegorov.knowledgeplatform.indexingservice.persistence.IndexedDocumentEntity;
import com.aegorov.knowledgeplatform.indexingservice.persistence.IndexedDocumentRepository;
import com.aegorov.knowledgeplatform.indexingservice.persistence.IndexingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexingService {

    private final StorageReader storageReader;
    private final DocumentTextExtractor textExtractor;
    private final TextChunker textChunker;
    private final EmbeddingModel embeddingModel;
    private final IndexedDocumentRepository indexedDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentChunkWriter documentChunkWriter;
    private final Clock clock;

    @Transactional
    public void index(DocumentUploadedV1 event) {
        if (indexedDocumentRepository.existsByLastEventId(event.eventId())) {
            log.debug("Skipping already processed event {}", event.eventId());
            return;
        }

        Instant now = clock.instant();
        IndexedDocumentEntity indexedDocument = indexedDocumentRepository.findById(event.documentId())
                .orElseGet(() -> IndexedDocumentEntity.builder()
                        .documentId(event.documentId())
                        .build());

        indexedDocument
                .setFileName(event.fileName())
                .setContentType(event.contentType())
                .setStorageKey(event.storageKey())
                .setStatus(IndexingStatus.PROCESSING)
                .setUpdatedAt(now)
                .setLastEventId(event.eventId());
        indexedDocumentRepository.saveAndFlush(indexedDocument);

        byte[] content = storageReader.read(event.storageKey());
        String extractedText = textExtractor.extract(content, event.contentType(), event.fileName());
        List<String> chunks = textChunker.chunk(extractedText);

        documentChunkRepository.deleteByDocumentId(event.documentId());
        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);
            float[] embedding = embeddingModel.embed(chunkContent);
            documentChunkWriter.insert(UUID.randomUUID(), event.documentId(), i, chunkContent, embedding, now);
        }

        indexedDocument
                .setStatus(IndexingStatus.INDEXED)
                .setChunkCount(chunks.size())
                .setIndexedAt(now)
                .setUpdatedAt(now);
        indexedDocumentRepository.save(indexedDocument);
        log.info("Indexed document {} with {} chunks", event.documentId(), chunks.size());
    }
}
