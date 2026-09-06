package com.aegorov.knowledgeplatform.ragservice.application.rag.dto;

import java.util.UUID;

/**
 * A source attribution entry emitted in the {@code SOURCES} chunk and returned by
 * the diagnostics search endpoint.
 *
 * @param documentId owning document.
 * @param fileName   the owning document's file name (may be {@code null} if unknown).
 * @param link       a resolvable link to the owning document (may be {@code null}).
 * @param chunkIndex chunk position within the document.
 * @param score      cosine similarity, higher is closer.
 * @param snippet    a short preview of the chunk text.
 */
public record SourceRef(
        UUID documentId,
        String fileName,
        String link,
        int chunkIndex,
        double score,
        String snippet
) {
}
