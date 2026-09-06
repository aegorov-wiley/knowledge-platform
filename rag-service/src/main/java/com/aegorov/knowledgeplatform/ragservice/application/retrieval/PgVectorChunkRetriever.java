package com.aegorov.knowledgeplatform.ragservice.application.retrieval;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * pgvector-backed {@link ChunkRetriever}. Reads (never writes) the
 * {@code indexing_service.document_chunks} table owned by indexing-service and
 * ranks rows by cosine distance ({@code <=>}) against the query embedding.
 * <p>
 * The embedding is passed as a pgvector text literal and cast to {@code vector}
 * in SQL, mirroring how indexing-service writes it. {@code score} is reported as
 * {@code 1 - cosine_distance} so higher means more similar.
 */
@Repository
public class PgVectorChunkRetriever implements ChunkRetriever {

    private static final String SEARCH_SQL = """
            SELECT c.document_id,
                   c.chunk_index,
                   c.content,
                   d.file_name AS file_name,
                   1 - (c.embedding <=> CAST(:queryEmbedding AS vector)) AS score
            FROM indexing_service.document_chunks c
            LEFT JOIN indexing_service.indexed_documents d ON d.document_id = c.document_id
            WHERE c.embedding IS NOT NULL
            ORDER BY c.embedding <=> CAST(:queryEmbedding AS vector)
            LIMIT :topK
            """;

    private final JdbcClient jdbcClient;

    public PgVectorChunkRetriever(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<RetrievedChunk> search(float[] queryEmbedding, int topK) {
        return jdbcClient.sql(SEARCH_SQL)
                .param("queryEmbedding", toVectorLiteral(queryEmbedding))
                .param("topK", topK)
                .query((rs, rowNum) -> new RetrievedChunk(
                        rs.getObject("document_id", UUID.class),
                        rs.getInt("chunk_index"),
                        rs.getString("content"),
                        rs.getDouble("score"),
                        rs.getString("file_name"),
                        null))
                .list();
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
