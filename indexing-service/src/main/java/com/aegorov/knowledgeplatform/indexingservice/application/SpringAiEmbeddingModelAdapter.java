package com.aegorov.knowledgeplatform.indexingservice.application;

import java.util.List;

/**
 * Adapts a Spring AI {@link org.springframework.ai.embedding.EmbeddingModel}
 * (e.g. Ollama, OpenAI) to the local {@link EmbeddingModel} domain interface,
 * keeping the indexing pipeline provider-agnostic.
 */
public class SpringAiEmbeddingModelAdapter implements EmbeddingModel {

    private final org.springframework.ai.embedding.EmbeddingModel delegate;

    public SpringAiEmbeddingModelAdapter(org.springframework.ai.embedding.EmbeddingModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public float[] embed(String text) {
        return delegate.embed(text);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return delegate.embed(texts);
    }

    @Override
    public int dimensions() {
        return delegate.dimensions();
    }
}
