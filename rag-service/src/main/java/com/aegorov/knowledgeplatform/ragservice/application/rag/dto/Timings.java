package com.aegorov.knowledgeplatform.ragservice.application.rag.dto;

/**
 * Per-stage timing diagnostics carried in the terminal {@code DONE} chunk.
 */
public record Timings(
        long embeddingMs,
        long retrievalMs,
        long llmMs,
        long totalMs
) {
}
