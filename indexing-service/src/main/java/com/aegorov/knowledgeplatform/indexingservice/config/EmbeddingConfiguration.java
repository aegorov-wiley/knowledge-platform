package com.aegorov.knowledgeplatform.indexingservice.config;

import com.aegorov.knowledgeplatform.indexingservice.application.EmbeddingModel;
import com.aegorov.knowledgeplatform.indexingservice.application.SpringAiEmbeddingModelAdapter;
import com.aegorov.knowledgeplatform.indexingservice.application.StubEmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the active {@link EmbeddingModel} via
 * {@code knowledge-platform.embedding.provider}:
 * <ul>
 *   <li>{@code stub} (default) — deterministic offline vectors, no external service.</li>
 *   <li>{@code ollama} — local Ollama via Spring AI ({@code spring.ai.ollama.*}).</li>
 *   <li>{@code openai} — OpenAI (e.g. text-embedding-3-small) via Spring AI
 *       ({@code spring.ai.openai.*}); dimensions truncated to match the column.</li>
 * </ul>
 * The Spring AI providers are injected by their concrete model type so multiple
 * starters can coexist on the classpath without ambiguous {@code EmbeddingModel}
 * beans.
 */
@Configuration
public class EmbeddingConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "knowledge-platform.embedding", name = "provider",
            havingValue = "stub", matchIfMissing = true)
    EmbeddingModel stubEmbeddingModel(EmbeddingProperties embeddingProperties) {
        return new StubEmbeddingModel(embeddingProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "knowledge-platform.embedding", name = "provider",
            havingValue = "ollama")
    EmbeddingModel ollamaEmbeddingModelAdapter(OllamaEmbeddingModel delegate) {
        return new SpringAiEmbeddingModelAdapter(delegate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "knowledge-platform.embedding", name = "provider",
            havingValue = "openai")
    EmbeddingModel openAiEmbeddingModelAdapter(OpenAiEmbeddingModel delegate) {
        return new SpringAiEmbeddingModelAdapter(delegate);
    }
}
