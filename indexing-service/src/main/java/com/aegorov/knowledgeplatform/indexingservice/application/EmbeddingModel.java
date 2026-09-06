package com.aegorov.knowledgeplatform.indexingservice.application;

import java.util.List;

/**
 * Generates vector embeddings for chunk text.
 * <p>
 * A thin domain seam over embedding generation so the current offline
 * {@link StubEmbeddingModel} can be swapped for a real Spring AI
 * {@code EmbeddingModel} (Azure OpenAI, OpenAI, Ollama, ...) without touching
 * the indexing pipeline.
 */
public interface EmbeddingModel {

    float[] embed(String text);

    List<float[]> embed(List<String> texts);

    int dimensions();
}
