package com.aegorov.knowledgeplatform.ragservice.application.llm;

import reactor.core.publisher.Flux;

/**
 * PLACEHOLDER Azure OpenAI {@link ChatLlm}, wired when
 * {@code knowledge-platform.llm.provider=azure}.
 * <p>
 * It constructs from configuration (endpoint / api-key / chat deployment) but
 * does NOT ship the Azure SDK, so {@link #stream(LlmContext)} fails fast with a
 * clear message. To make it real, add the Spring AI Azure OpenAI starter and
 * build a streaming {@code ChatClient} call here. No endpoint or key is ever
 * committed; values come from environment variables with empty defaults.
 */
public class AzureOpenAiChatLlm implements ChatLlm {

    private final String endpoint;
    private final String deployment;

    public AzureOpenAiChatLlm(String endpoint, String deployment) {
        this.endpoint = endpoint;
        this.deployment = deployment;
    }

    @Override
    public Flux<String> stream(LlmContext context) {
        return Flux.error(new IllegalStateException(
                "Azure OpenAI chat provider is a placeholder (endpoint='%s', deployment='%s'). "
                        .formatted(endpoint, deployment)
                        + "Add the Spring AI Azure OpenAI starter and wire a streaming ChatClient to enable it, "
                        + "or set knowledge-platform.llm.provider=stub for the offline demo."));
    }
}
