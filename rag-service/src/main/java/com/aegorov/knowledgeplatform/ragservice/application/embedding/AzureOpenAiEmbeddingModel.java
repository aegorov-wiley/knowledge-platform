package com.aegorov.knowledgeplatform.ragservice.application.embedding;

/**
 * PLACEHOLDER Azure OpenAI embedding model, wired when
 * {@code knowledge-platform.embedding.provider=azure}.
 * <p>
 * It is intentionally inert: it constructs from configuration (endpoint / api-key /
 * deployment) but does NOT ship the Azure SDK, so calling {@link #embed(String)}
 * fails fast with a clear message. To make it real, add the Spring AI Azure OpenAI
 * starter and replace the body of {@link #embed(String)} with a call to the
 * injected {@code AzureOpenAiEmbeddingModel}. No endpoint or key is ever committed;
 * all values come from environment variables with empty defaults.
 */
public class AzureOpenAiEmbeddingModel implements EmbeddingModel {

    private final int dimensions;
    private final String endpoint;
    private final String deployment;

    public AzureOpenAiEmbeddingModel(int dimensions, String endpoint, String deployment) {
        this.dimensions = dimensions;
        this.endpoint = endpoint;
        this.deployment = deployment;
    }

    @Override
    public float[] embed(String text) {
        throw new IllegalStateException(
                "Azure OpenAI embedding provider is a placeholder (endpoint='%s', deployment='%s'). "
                        .formatted(endpoint, deployment)
                        + "Add the Spring AI Azure OpenAI starter and wire the real client to enable it, "
                        + "or set knowledge-platform.embedding.provider=stub for the offline demo.");
    }

    @Override
    public int dimensions() {
        return dimensions;
    }
}
