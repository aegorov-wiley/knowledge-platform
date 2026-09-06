package com.aegorov.knowledgeplatform.ragservice.application.llm;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.aegorov.knowledgeplatform.ragservice.application.retrieval.RetrievedChunk;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class StubChatLlmTest {

    private final StubChatLlm stubChatLlm = new StubChatLlm();

    @Test
    void streamsMultipleFragmentsThatCiteSources() {
        UUID docId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LlmContext context = new LlmContext(
                "system",
                List.of(),
                List.of(new RetrievedChunk(docId, 3, "Spring Boot auto-configures beans based on the classpath.", 0.91)),
                "How does Spring Boot configuration work?");

        String answer = collect(stubChatLlm.stream(context));

        assertThat(answer).contains(docId.toString());
        assertThat(answer).contains("chunk #3");
        assertThat(answer).contains("Spring Boot auto-configures beans");

        long fragmentCount = stubChatLlm.stream(context).count().block();
        assertThat(fragmentCount).isGreaterThan(1);
    }

    @Test
    void isDeterministic() {
        LlmContext context = new LlmContext("system", List.of(),
                List.of(new RetrievedChunk(UUID.randomUUID(), 0, "content", 0.5)), "q");

        assertThat(collect(stubChatLlm.stream(context))).isEqualTo(collect(stubChatLlm.stream(context)));
    }

    @Test
    void reportsWhenNoSourcesWereRetrieved() {
        LlmContext context = new LlmContext("system", List.of(), List.of(), "unknown topic");

        String answer = collect(stubChatLlm.stream(context));

        assertThat(answer).containsIgnoringCase("could not find any relevant indexed sources");
    }

    @Test
    void emitsAtLeastOneFragment() {
        LlmContext context = new LlmContext("system", List.of(), List.of(), "q");
        StepVerifier.create(stubChatLlm.stream(context).hasElements())
                .expectNext(true)
                .verifyComplete();
    }

    private static String collect(Flux<String> stream) {
        return String.join("", stream.collectList().block());
    }
}
