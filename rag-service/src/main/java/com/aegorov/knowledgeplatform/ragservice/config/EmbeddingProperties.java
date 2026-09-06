package com.aegorov.knowledgeplatform.ragservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param provider   embedding provider: {@code stub} (default, offline) or {@code azure} (placeholder).
 * @param dimensions vector size; MUST match the {@code indexing_service.document_chunks.embedding}
 *                   column and the indexing-service stub, otherwise a query embedding cannot be
 *                   compared against stored chunk embeddings.
 */
@ConfigurationProperties(prefix = "knowledge-platform.embedding")
public record EmbeddingProperties(
        String provider,
        int dimensions
) {

    public EmbeddingProperties {
        if (dimensions <= 0) {
            dimensions = 768;
        }
    }
}
