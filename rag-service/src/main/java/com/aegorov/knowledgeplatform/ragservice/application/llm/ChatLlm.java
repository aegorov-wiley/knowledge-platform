package com.aegorov.knowledgeplatform.ragservice.application.llm;

import reactor.core.publisher.Flux;

/**
 * Streaming chat completion seam.
 * <p>
 * Implementations emit the answer as a stream of token fragments so the UI can
 * render it progressively. The default {@link StubChatLlm} is fully offline; an
 * Azure OpenAI implementation is wired as a placeholder behind
 * {@code knowledge-platform.llm.provider=azure}.
 */
public interface ChatLlm {

    Flux<String> stream(LlmContext context);
}
