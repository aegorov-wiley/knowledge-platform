package com.aegorov.knowledgeplatform.indexingservice.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Writes document chunks together with their pgvector embedding.
 * <p>
 * Uses {@link JdbcClient} because the {@code embedding} column is a pgvector
 * {@code vector} type, which is not natively mapped by JPA/Hibernate. The
 * embedding is passed as its text literal and cast to {@code vector} in SQL.
 */
@Repository
public class DocumentChunkWriter {

    private static final String INSERT_SQL = """
            INSERT INTO indexing_service.document_chunks
                (id, document_id, chunk_index, content, embedding, created_at)
            VALUES (:id, :documentId, :chunkIndex, :content, CAST(:embedding AS vector), :createdAt)
            """;

    private final JdbcClient jdbcClient;

    public DocumentChunkWriter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void insert(UUID id, UUID documentId, int chunkIndex, String content, float[] embedding, Instant createdAt) {
        jdbcClient.sql(INSERT_SQL)
                .param("id", id)
                .param("documentId", documentId)
                .param("chunkIndex", chunkIndex)
                .param("content", content)
                .param("embedding", toVectorLiteral(embedding))
                .param("createdAt", OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC))
                .update();
    }

    private String toVectorLiteral(float[] embedding) {
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
