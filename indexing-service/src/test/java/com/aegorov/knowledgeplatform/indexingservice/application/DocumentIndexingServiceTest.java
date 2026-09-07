package com.aegorov.knowledgeplatform.indexingservice.application;

import com.aegorov.knowledgeplatform.indexingservice.messaging.DocumentUploadedV1;
import com.aegorov.knowledgeplatform.indexingservice.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIndexingServiceTest {

    private static final String EVENT_ID = "evt-123";
    private static final UUID DOCUMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String FILE_NAME = "doc.pdf";
    private static final String CONTENT_TYPE = "application/pdf";
    private static final String STORAGE_KEY = "documents/11111111-1111-1111-1111-111111111111/doc.pdf";
    private static final Instant NOW = Instant.parse("2026-09-07T12:00:00Z");

    @Mock
    private StorageReader storageReader;
    @Mock
    private DocumentTextExtractor textExtractor;
    @Mock
    private TextChunker textChunker;
    @Mock
    private EmbeddingModel embeddingModel;
    @Mock
    private IndexedDocumentRepository indexedDocumentRepository;
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    @Mock
    private DocumentChunkWriter documentChunkWriter;
    @Spy
    private final Clock clock = Clock.fixed(NOW, ZoneId.of("UTC"));

    @InjectMocks
    private DocumentIndexingService documentIndexingService;

    @Test
    void indexesDocumentSuccessfully() {
        var event = new DocumentUploadedV1(
                EVENT_ID,
                DOCUMENT_ID,
                FILE_NAME,
                CONTENT_TYPE,
                1024L,
                STORAGE_KEY,
                NOW
        );
        byte[] rawBytes = "pdf-content".getBytes();
        String extractedText = "Extracted document text";
        List<String> chunks = List.of("Chunk 1", "Chunk 2");
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

        when(indexedDocumentRepository.existsByLastEventId(EVENT_ID)).thenReturn(false);
        when(indexedDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());
        when(storageReader.read(STORAGE_KEY)).thenReturn(rawBytes);
        when(textExtractor.extract(rawBytes, CONTENT_TYPE, FILE_NAME)).thenReturn(extractedText);
        when(textChunker.chunk(extractedText)).thenReturn(chunks);
        when(embeddingModel.embed(any(String.class))).thenReturn(embedding);
        when(indexedDocumentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        documentIndexingService.index(event);

        verify(documentChunkRepository).deleteByDocumentId(DOCUMENT_ID);
        verify(documentChunkWriter).insert(any(UUID.class), eq(DOCUMENT_ID), eq(0), eq("Chunk 1"), eq(embedding), eq(NOW));
        verify(documentChunkWriter).insert(any(UUID.class), eq(DOCUMENT_ID), eq(1), eq("Chunk 2"), eq(embedding), eq(NOW));

        verify(indexedDocumentRepository).save(org.mockito.ArgumentMatchers.argThat((IndexedDocumentEntity doc) ->
                doc.getDocumentId().equals(DOCUMENT_ID) &&
                        doc.getFileName().equals(FILE_NAME) &&
                        doc.getContentType().equals(CONTENT_TYPE) &&
                        doc.getStorageKey().equals(STORAGE_KEY) &&
                        doc.getStatus() == IndexingStatus.INDEXED &&
                        doc.getChunkCount() == 2 &&
                        doc.getIndexedAt().equals(NOW) &&
                        doc.getLastEventId().equals(EVENT_ID)
        ));
    }
}
