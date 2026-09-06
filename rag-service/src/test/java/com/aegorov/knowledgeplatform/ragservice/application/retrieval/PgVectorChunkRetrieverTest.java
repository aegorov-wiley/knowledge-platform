package com.aegorov.knowledgeplatform.ragservice.application.retrieval;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aegorov.knowledgeplatform.ragservice.TestcontainersConfiguration;
import com.aegorov.knowledgeplatform.ragservice.application.embedding.StubEmbeddingModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies vector similarity ranking against a fixture that stands in for
 * indexing-service's {@code document_chunks} table. Because stub embeddings are
 * deterministic per exact text, a query whose text matches a stored chunk yields
 * an (almost) perfect cosine score and must rank first.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PgVectorChunkRetrieverTest {

    private static final int DIMENSIONS = 768;
    private static final UUID RELEVANT_DOC = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_DOC = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final String RELEVANT_TEXT = "What are common interview questions about Java concurrency?";
    private static final String OTHER_TEXT = "A recipe for growing tomatoes in a home garden.";

    private final StubEmbeddingModel embeddingModel = new StubEmbeddingModel(DIMENSIONS);

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private ChunkRetriever chunkRetriever;

    @BeforeEach
    void setUpFixture() {
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
        jdbcClient.sql("TRUNCATE indexing_service.indexed_documents").update();
        insertDocument(RELEVANT_DOC, "java-concurrency.txt");
        insertDocument(OTHER_DOC, "tomatoes.txt");
        insertChunk(RELEVANT_DOC, 0, RELEVANT_TEXT);
        insertChunk(OTHER_DOC, 0, OTHER_TEXT);
    }

    @Test
    void ranksTheChunkMatchingTheQueryFirst() {
        float[] queryEmbedding = embeddingModel.embed(RELEVANT_TEXT);

        List<RetrievedChunk> results = chunkRetriever.search(queryEmbedding, 5);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).documentId()).isEqualTo(RELEVANT_DOC);
        assertThat(results.get(0).fileName()).isEqualTo("java-concurrency.txt");
        assertThat(results.get(0).score()).isGreaterThan(results.get(1).score());
        assertThat(results.get(0).score()).isCloseTo(1.0, org.assertj.core.api.Assertions.within(1e-4));
    }

    @Test
    void honoursTopKLimit() {
        float[] queryEmbedding = embeddingModel.embed(RELEVANT_TEXT);

        assertThat(chunkRetriever.search(queryEmbedding, 1)).hasSize(1);
    }

    private void insertDocument(UUID documentId, String fileName) {
        jdbcClient.sql("""
                        INSERT INTO indexing_service.indexed_documents (document_id, file_name)
                        VALUES (:documentId, :fileName)
                        """)
                .param("documentId", documentId)
                .param("fileName", fileName)
                .update();
    }

    private void insertChunk(UUID documentId, int chunkIndex, String content) {
        jdbcClient.sql("""
                        INSERT INTO indexing_service.document_chunks
                            (id, document_id, chunk_index, content, embedding, created_at)
                        VALUES (:id, :documentId, :chunkIndex, :content, CAST(:embedding AS vector), :createdAt)
                        """)
                .param("id", UUID.randomUUID())
                .param("documentId", documentId)
                .param("chunkIndex", chunkIndex)
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
