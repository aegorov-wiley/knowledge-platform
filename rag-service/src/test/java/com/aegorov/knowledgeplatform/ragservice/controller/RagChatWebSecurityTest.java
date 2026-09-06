package com.aegorov.knowledgeplatform.ragservice.controller;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.aegorov.knowledgeplatform.ragservice.TestcontainersConfiguration;
import com.aegorov.knowledgeplatform.ragservice.application.embedding.StubEmbeddingModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of the RAG HTTP surface: role-based security plus the
 * streaming chat and diagnostics search endpoints, against a fixture chunk table.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RagChatWebSecurityTest {

    private static final String QUESTION = "What are common interview questions about Java?";

    private final StubEmbeddingModel embeddingModel = new StubEmbeddingModel(768);

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(30))
                .build();
        jdbcClient.sql("CREATE EXTENSION IF NOT EXISTS vector").update();
        jdbcClient.sql("CREATE SCHEMA IF NOT EXISTS indexing_service").update();
        jdbcClient.sql("""
                CREATE TABLE IF NOT EXISTS indexing_service.indexed_documents (
                    document_id UUID PRIMARY KEY,
                    file_name VARCHAR(255) NOT NULL
                )
                """).update();
        jdbcClient.sql("""
                CREATE TABLE IF NOT EXISTS indexing_service.document_chunks (
                    id UUID PRIMARY KEY,
                    document_id UUID NOT NULL,
                    chunk_index INT NOT NULL,
                    content TEXT NOT NULL,
                    embedding vector(768),
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """).update();
        jdbcClient.sql("TRUNCATE indexing_service.document_chunks").update();
        insertChunk(UUID.randomUUID(), QUESTION);
    }

    @Test
    void searchRequiresAuthentication() {
        webTestClient.post().uri("/api/v1/rag/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"%s\"}".formatted(QUESTION))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void authenticatedUserCanSearch() {
        webTestClient.post().uri("/api/v1/rag/search")
                .headers(headers -> headers.setBasicAuth("user", "user"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"%s\"}".formatted(QUESTION))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].documentId").exists();
    }

    @Test
    void authenticatedUserCanChatAndReceivesStreamedProtocol() {
        String body = webTestClient.post().uri("/api/v1/rag/chat")
                .headers(headers -> headers.setBasicAuth("admin", "admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON)
                .bodyValue("{\"question\":\"%s\"}".formatted(QUESTION))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).contains("\"type\":\"META\"");
        assertThat(body).contains("\"type\":\"SOURCES\"");
        assertThat(body).contains("\"type\":\"DELTA\"");
        assertThat(body).contains("\"type\":\"DONE\"");
    }

    @Test
    void blankQuestionIsRejected() {
        webTestClient.post().uri("/api/v1/rag/chat")
                .headers(headers -> headers.setBasicAuth("user", "user"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"  \"}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    private void insertChunk(UUID documentId, String content) {
        jdbcClient.sql("""
                        INSERT INTO indexing_service.document_chunks
                            (id, document_id, chunk_index, content, embedding, created_at)
                        VALUES (:id, :documentId, 0, :content, CAST(:embedding AS vector), :createdAt)
                        """)
                .param("id", UUID.randomUUID())
                .param("documentId", documentId)
                .param("content", content)
                .param("embedding", toVectorLiteral(embeddingModel.embed(content)))
                .param("createdAt", OffsetDateTime.now(ZoneOffset.UTC))
                .update();
    }

    private static String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder(embedding.length * 8 + 2);
        builder.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding[i]);
        }
        builder.append(']');
        return builder.toString();
    }
}
