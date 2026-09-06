package com.aegorov.knowledgeplatform.ragservice.application.embedding;

/**
 * Generates a vector embedding for a piece of text.
 * <p>
 * A thin domain seam so the offline {@link StubEmbeddingModel} can be swapped for
 * a real provider (Azure OpenAI, ...) without touching the RAG pipeline. This is
 * deliberately re-declared inside rag-service rather than shared with
 * indexing-service: the platform forbids a shared domain module, so each service
 * owns its own copy of the seam.
 */
public interface EmbeddingModel {

    float[] embed(String text);

    int dimensions();
}
