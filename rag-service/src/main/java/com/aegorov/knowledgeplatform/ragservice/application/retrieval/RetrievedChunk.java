package com.aegorov.knowledgeplatform.ragservice.application.retrieval;

import java.util.UUID;

/**
 * A single chunk returned by vector similarity search.
 *
 * @param documentId owning document (from indexing-service).
 * @param chunkIndex position of the chunk within its document.
 * @param content    the chunk text.
 * @param score      cosine similarity in roughly [-1, 1]; higher is closer.
 * @param fileName   the owning document's file name (may be {@code null} if unknown).
 * @param link       a resolvable link to the owning document (may be {@code null}).
 */
public record RetrievedChunk(
        UUID documentId,
        int chunkIndex,
        String content,
        double score,
        String fileName,
        String link
) {

    /**
     * Convenience constructor for callers/tests that only have the core fields;
     * {@code fileName} and {@code link} default to {@code null}.
     */
    public RetrievedChunk(UUID documentId, int chunkIndex, String content, double score) {
        this(documentId, chunkIndex, content, score, null, null);
    }

    /** Returns a copy of this chunk with the given resolvable document link. */
    public RetrievedChunk withLink(String newLink) {
        return new RetrievedChunk(documentId, chunkIndex, content, score, fileName, newLink);
    }
}
