package com.aegorov.knowledgeplatform.documentservice;

import com.aegorov.knowledgeplatform.TestcontainersConfiguration;
import com.aegorov.knowledgeplatform.documentservice.application.service.DocumentPersistenceService;
import com.aegorov.knowledgeplatform.documentservice.messaging.OutboxPublisherService;
import com.aegorov.knowledgeplatform.documentservice.persistence.DocumentRepository;
import com.aegorov.knowledgeplatform.documentservice.persistence.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "knowledge-platform.storage.local.root=target/test-storage",
        "knowledge-platform.documents.max-size=1KB",
        "spring.servlet.multipart.max-file-size=1KB",
        "spring.servlet.multipart.max-request-size=1KB"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DocumentServiceIntegrationTest {

    private static final String ENDPOINT = "/api/v1/documents";
    private static final String TOPIC = "knowledge.documents.v1";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    OutboxPublisherService outboxPublisherService;

    @Autowired
    DocumentPersistenceService documentPersistenceService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaContainer kafkaContainer;

    @BeforeEach
    void cleanState() throws IOException {
        outboxEventRepository.deleteAll();
        documentRepository.deleteAll();
        deleteStorageRoot(Path.of("target/test-storage"));
    }

    @Test
    void uploadsDocumentAndPersistsMetadataAndOutboxEvent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "architecture.pdf",
                "application/pdf",
                "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        var result = mockMvc.perform(multipart(ENDPOINT).file(file).with(httpBasic("admin", "admin")))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fileName").value("architecture.pdf"))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        UUID documentId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText()
        );

        var document = documentRepository.findById(documentId).orElseThrow();
        Assertions.assertThat(document.getFileName()).isEqualTo("architecture.pdf");
        Assertions.assertThat(outboxEventRepository.findAll()).hasSize(1);
        Assertions.assertThat(Files.exists(Path.of("target/test-storage").resolve(document.getStorageKey()))).isTrue();
    }

    @Test
    void rejectsUploadForNonAdminRole() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "architecture.pdf",
                "application/pdf",
                "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart(ENDPOINT).file(file).with(httpBasic("user", "user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnsupportedContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(multipart(ENDPOINT).file(file).with(httpBasic("admin", "admin")))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));

        Assertions.assertThat(documentRepository.count()).isZero();
        Assertions.assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void rejectsOversizedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                new byte[2048]
        );

        mockMvc.perform(multipart(ENDPOINT).file(file).with(httpBasic("admin", "admin")))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));

        Assertions.assertThat(documentRepository.count()).isZero();
        Assertions.assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void publishesOutboxEventToKafkaAndMarksItPublished() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "architecture.pdf",
                "application/pdf",
                "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        var result = mockMvc.perform(multipart(ENDPOINT).file(file).with(httpBasic("admin", "admin")))
                .andExpect(status().isAccepted())
                .andReturn();

        UUID documentId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText()
        );

        outboxPublisherService.publishDueEvents();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties())) {
            consumer.subscribe(List.of(TOPIC));
            ConsumerRecord<String, String> record = pollForRecord(consumer, TOPIC, documentId.toString());
            Assertions.assertThat(record.key()).isEqualTo(documentId.toString());
            Assertions.assertThat(record.value()).contains("architecture.pdf");
        }

        Assertions.assertThat(outboxEventRepository.findAll()).singleElement()
                .matches(event -> event.getPublishedAt() != null);
    }

    @Test
    void uploadingSameFileNameTwiceUpdatesExistingDocument() throws Exception {
        MockMultipartFile initialFile = new MockMultipartFile(
                "file",
                "architecture.pdf",
                "application/pdf",
                "first-content".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile updatedFile = new MockMultipartFile(
                "file",
                "architecture.pdf",
                "application/pdf",
                "second-content".getBytes(StandardCharsets.UTF_8)
        );

        var firstResult = mockMvc.perform(multipart(ENDPOINT).file(initialFile).with(httpBasic("admin", "admin")))
                .andExpect(status().isAccepted())
                .andReturn();
        var secondResult = mockMvc.perform(multipart(ENDPOINT).file(updatedFile).with(httpBasic("admin", "admin")))
                .andExpect(status().isAccepted())
                .andReturn();

        var firstDocumentId = objectMapper.readTree(firstResult.getResponse().getContentAsString()).get("id").asText();
        var secondDocumentId = objectMapper.readTree(secondResult.getResponse().getContentAsString()).get("id").asText();

        Assertions.assertThat(secondDocumentId).isEqualTo(firstDocumentId);
        Assertions.assertThat(documentRepository.count()).isEqualTo(1);
        Assertions.assertThat(outboxEventRepository.count()).isEqualTo(2);
        var document = documentRepository.findById(UUID.fromString(firstDocumentId)).orElseThrow();
        Assertions.assertThat(document.getSize()).isEqualTo(updatedFile.getSize());
    }

    private Properties consumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "knowledge-platform-test-" + UUID.randomUUID());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return props;
    }

    private ConsumerRecord<String, String> pollForRecord(KafkaConsumer<String, String> consumer, String topic, String key) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records.records(topic)) {
                if (key.equals(record.key())) {
                    log.info("Received test-record: key:{}, value:{}", record.key(), record.value());
                    return record;
                }
            }
        }
        throw new AssertionError("No Kafka record found for key " + key);
    }

    private void deleteStorageRoot(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                    });
        }
    }
}
