package com.aegorov.knowledgeplatform.documentservice.application.service;

import com.aegorov.knowledgeplatform.documentservice.application.dto.DocumentUploadResponse;
import com.aegorov.knowledgeplatform.documentservice.application.port.FileStorage;
import com.aegorov.knowledgeplatform.documentservice.persistence.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    private static final String FILE_NAME = "doc.txt";
    private static final String CONTENT_TYPE = MediaType.TEXT_PLAIN_VALUE;
    private static final byte[] CONTENT = "hello".getBytes();
    private static final UUID DOCUMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String STORAGE_KEY = "documents/" + DOCUMENT_ID + "/" + FILE_NAME;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private DocumentPersistenceService documentPersistenceService;

    @Mock
    private FileValidationServiceImpl fileValidationService;

    @InjectMocks
    private DocumentUploadService documentUploadService;

    @Test
    void uploadStoresFileAndCreatesDocumentMetadata() throws Exception {
        var file = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, CONTENT);
        var uploadTarget = DocumentPersistenceService.UploadTarget.builder()
                .documentId(DOCUMENT_ID)
                .storageKey(STORAGE_KEY)
                .build();
        var expectedResponse = new DocumentUploadResponse(
                DOCUMENT_ID,
                FILE_NAME,
                DocumentStatus.UPLOADED,
                Instant.now()
        );

        when(documentPersistenceService.resolveUploadTarget(FILE_NAME)).thenReturn(uploadTarget);
        when(documentPersistenceService.createDocument(
                DOCUMENT_ID,
                FILE_NAME,
                CONTENT_TYPE,
                CONTENT.length,
                STORAGE_KEY
        )).thenReturn(expectedResponse);

        DocumentUploadResponse actualResponse = documentUploadService.upload(file);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(fileValidationService).validate(any(), eq(file));
        verify(fileStorage).store(eq(STORAGE_KEY), any(InputStream.class), eq((long) CONTENT.length), eq(CONTENT_TYPE));
        verify(documentPersistenceService)
                .createDocument(DOCUMENT_ID, FILE_NAME, CONTENT_TYPE, CONTENT.length, STORAGE_KEY);
    }
}
