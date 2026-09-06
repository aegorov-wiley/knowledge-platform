package com.aegorov.knowledgeplatform.documentservice;

import com.aegorov.knowledgeplatform.TestcontainersConfiguration;
import com.aegorov.knowledgeplatform.documentservice.application.service.DocumentPersistenceService;
import com.aegorov.knowledgeplatform.documentservice.persistence.DocumentRepository;
import com.aegorov.knowledgeplatform.documentservice.persistence.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Import(TestcontainersConfiguration.class)
class DocumentPersistenceAtomicityTest {

    @Autowired
    DocumentPersistenceService documentPersistenceService;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @MockitoBean
    ObjectMapper objectMapper;

    @BeforeEach
    void cleanState() {
        outboxEventRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Test
    void rollsBackDocumentWhenOutboxSerializationFails() throws Exception {
        doThrow(new RuntimeException("boom")).when(objectMapper).writeValueAsString(any());

        try {
            documentPersistenceService.createDocument(
                    UUID.randomUUID(),
                    "architecture.pdf",
                    "application/pdf",
                    123L,
                    "documents/test/architecture.pdf"
            );
        } catch (RuntimeException ex) {
            org.assertj.core.api.Assertions.assertThat(ex).hasMessageContaining("boom");
        }

        org.assertj.core.api.Assertions.assertThat(documentRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(outboxEventRepository.count()).isZero();
    }
}
