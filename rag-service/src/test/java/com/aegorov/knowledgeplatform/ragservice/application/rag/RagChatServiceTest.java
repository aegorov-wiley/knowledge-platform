package com.aegorov.knowledgeplatform.ragservice.application.rag;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aegorov.knowledgeplatform.ragservice.application.ChatMemoryProvider;
import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationHistoryResponse;
import com.aegorov.knowledgeplatform.ragservice.application.embedding.EmbeddingModel;
import com.aegorov.knowledgeplatform.ragservice.application.llm.ChatLlm;
import com.aegorov.knowledgeplatform.ragservice.application.rag.dto.RagChatChunk;
import com.aegorov.knowledgeplatform.ragservice.application.retrieval.ChunkRetriever;
import com.aegorov.knowledgeplatform.ragservice.application.retrieval.RetrievedChunk;
import com.aegorov.knowledgeplatform.ragservice.config.RagProperties;
import com.aegorov.knowledgeplatform.ragservice.persistence.MessageRole;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagChatServiceTest {

    private static final String USER = "user-1";
    private static final UUID CONVERSATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DOC_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private ChatMemoryProvider chatMemoryProvider;
    private EmbeddingModel embeddingModel;
    private ChunkRetriever chunkRetriever;
    private ChatLlm chatLlm;
    private RagChatService service;

    @BeforeEach
    void setUp() {
        chatMemoryProvider = mock(ChatMemoryProvider.class);
        embeddingModel = mock(EmbeddingModel.class);
        chunkRetriever = mock(ChunkRetriever.class);
        chatLlm = mock(ChatLlm.class);
        service = new RagChatService(chatMemoryProvider, embeddingModel, chunkRetriever, chatLlm,
                new RagProperties(5, 6000, null, null));

        when(chatMemoryProvider.createConversation(USER)).thenReturn(CONVERSATION_ID);
        when(chatMemoryProvider.getConversationMessages(CONVERSATION_ID, USER))
                .thenReturn(new ConversationHistoryResponse(CONVERSATION_ID, List.of()));
        when(embeddingModel.embed(any())).thenReturn(new float[768]);
        when(chunkRetriever.search(any(), anyInt()))
                .thenReturn(List.of(new RetrievedChunk(DOC_ID, 1, "relevant chunk text", 0.87)));
        when(chatLlm.stream(any())).thenReturn(Flux.just("Hello ", "world"));
    }

    @Test
    void emitsMetaSourcesDeltasThenDoneAndPersistsTurn() {
        StepVerifier.create(service.chat(USER, null, "What is this?"))
                .assertNext(chunk -> assertType(chunk, "META"))
                .assertNext(chunk -> assertType(chunk, "SOURCES"))
                .assertNext(chunk -> assertType(chunk, "DELTA"))
                .assertNext(chunk -> assertType(chunk, "DELTA"))
                .assertNext(chunk -> assertType(chunk, "DONE"))
                .verifyComplete();

        verify(chatMemoryProvider).appendMessage(CONVERSATION_ID, USER, MessageRole.USER, "What is this?");
        verify(chatMemoryProvider).appendMessage(CONVERSATION_ID, USER, MessageRole.ASSISTANT, "Hello world");
    }

    @Test
    void metaCarriesConversationIdAndSourcesCarryChunks() {
        List<RagChatChunk> chunks = service.chat(USER, null, "q").collectList().block();

        RagChatChunk meta = chunks.get(0);
        RagChatChunk sources = chunks.get(1);
        org.assertj.core.api.Assertions.assertThat(meta.conversationId()).isEqualTo(CONVERSATION_ID);
        org.assertj.core.api.Assertions.assertThat(sources.sources()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(sources.sources().get(0).documentId()).isEqualTo(DOC_ID);
    }

    @Test
    void generationErrorBecomesTerminalErrorChunk() {
        when(chatLlm.stream(any())).thenReturn(Flux.error(new RuntimeException("model exploded")));

        List<RagChatChunk> chunks = service.chat(USER, null, "q").collectList().block();

        assertType(chunks.get(0), "META");
        assertType(chunks.get(1), "SOURCES");
        assertType(chunks.get(chunks.size() - 1), "ERROR");
        // no assistant text was produced, so nothing is persisted for the assistant role
        verify(chatMemoryProvider, times(0))
                .appendMessage(eq(CONVERSATION_ID), eq(USER), eq(MessageRole.ASSISTANT), any());
    }

    private static void assertType(RagChatChunk chunk, String expected) {
        org.assertj.core.api.Assertions.assertThat(chunk.type()).isEqualTo(expected);
    }
}
