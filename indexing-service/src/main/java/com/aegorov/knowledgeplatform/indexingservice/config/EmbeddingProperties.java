package com.aegorov.knowledgeplatform.indexingservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param provider   embedding provider: {@code stub} (default, offline) or {@code ollama}.
 * @param dimensions vector size; must match the {@code document_chunks.embedding} column
 *                   and the selected Ollama model (e.g. nomic-embed-text = 768).
 */
@ConfigurationProperties(prefix = "knowledge-platform.embedding")
public record EmbeddingProperties(
        String provider,
        int dimensions
) {
}
