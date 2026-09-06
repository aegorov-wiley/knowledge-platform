package com.aegorov.knowledgeplatform.ragservice.application.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.aegorov.knowledgeplatform.ragservice.application.ChatMemoryProvider;
import com.aegorov.knowledgeplatform.ragservice.application.embedding.EmbeddingModel;
import com.aegorov.knowledgeplatform.ragservice.application.llm.ChatLlm;
import com.aegorov.knowledgeplatform.ragservice.application.llm.LlmContext;
import com.aegorov.knowledgeplatform.ragservice.application.rag.dto.RagChatChunk;
import com.aegorov.knowledgeplatform.ragservice.application.rag.dto.SourceRef;
import com.aegorov.knowledgeplatform.ragservice.application.rag.dto.Timings;
import com.aegorov.knowledgeplatform.ragservice.application.retrieval.ChunkRetriever;
import com.aegorov.knowledgeplatform.ragservice.application.retrieval.RetrievedChunk;
import com.aegorov.knowledgeplatform.ragservice.config.RagProperties;
import com.aegorov.knowledgeplatform.ragservice.persistence.MessageRole;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Orchestrates the RAG pipeline: embed the question, search indexed chunks,
 * assemble an LLM context, stream the answer, and persist the turn.
 * <p>
 * Retrieval, embedding and persistence are blocking (JDBC / JPA); they run on the
 * bounded-elastic scheduler so the reactive event loop is never blocked.
 * Preparation errors (blank question, unknown/forbidden conversation) surface
 * before streaming starts and become HTTP errors; errors DURING generation become
 * a terminal {@code ERROR} chunk.
 */
@Service
public class RagChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);

    private static final int SNIPPET_CHARS = 200;

    private final ChatMemoryProvider chatMemoryProvider;
    private final EmbeddingModel embeddingModel;
    private final ChunkRetriever chunkRetriever;
    private final ChatLlm chatLlm;
    private final RagProperties ragProperties;

    public RagChatService(ChatMemoryProvider chatMemoryProvider,
                          EmbeddingModel embeddingModel,
                          ChunkRetriever chunkRetriever,
                          ChatLlm chatLlm,
                          RagProperties ragProperties) {
        this.chatMemoryProvider = chatMemoryProvider;
        this.embeddingModel = embeddingModel;
        this.chunkRetriever = chunkRetriever;
        this.chatLlm = chatLlm;
        this.ragProperties = ragProperties;
    }

    public Flux<RagChatChunk> chat(String userId, UUID conversationId, String question) {
        return Mono.fromCallable(() -> prepare(userId, conversationId, question))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(this::streamAnswer);
    }

    public Mono<List<SourceRef>> search(String question, Integer topK) {
        int effectiveTopK = (topK == null || topK <= 0) ? ragProperties.topK() : topK;
        return Mono.fromCallable(() -> {
                    float[] embedding = embeddingModel.embed(question);
                    return chunkRetriever.search(embedding, effectiveTopK).stream()
                            .map(this::toSourceRef)
                            .toList();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
    private Prepared prepare(String userId, UUID conversationId, String question) {
        long startNanos = System.nanoTime();
        UUID resolvedId = conversationId != null
                ? conversationId
                : chatMemoryProvider.createConversation(userId);

        List<LlmContext.Turn> history = chatMemoryProvider.getConversationMessages(resolvedId, userId)
                .messages().stream()
                .map(message -> new LlmContext.Turn(message.role(), message.content()))
                .toList();

        chatMemoryProvider.appendMessage(resolvedId, userId, MessageRole.USER, question);

        long embedStart = System.nanoTime();
        float[] embedding = embeddingModel.embed(question);
        long embeddingMs = millisSince(embedStart);

        long retrievalStart = System.nanoTime();
        List<RetrievedChunk> chunks = chunkRetriever.search(embedding, ragProperties.topK()).stream()
                .map(chunk -> chunk.withLink(ragProperties.linkFor(chunk.documentId())))
                .toList();
        long retrievalMs = millisSince(retrievalStart);

        List<SourceRef> sources = chunks.stream().map(this::toSourceRef).toList();
        LlmContext context = new LlmContext(ragProperties.systemPrompt(), history, capContext(chunks), question);

        return new Prepared(resolvedId, userId, sources, context, embeddingMs, retrievalMs, startNanos);
    }

    private Flux<RagChatChunk> streamAnswer(Prepared prepared) {
        StringBuilder answer = new StringBuilder();
        long[] llmStart = {0L};

        Flux<RagChatChunk> head = Flux.just(
                RagChatChunk.meta(prepared.conversationId()),
                RagChatChunk.sources(prepared.sources()));

        Flux<RagChatChunk> body = Flux.defer(() -> {
                    llmStart[0] = System.nanoTime();
                    return chatLlm.stream(prepared.context());
                })
                .doOnNext(answer::append)
                .map(RagChatChunk::delta);

        Mono<RagChatChunk> tail = Mono.fromCallable(() -> {
                    persistAssistant(prepared, answer.toString());
                    long llmMs = millisSince(llmStart[0]);
                    long totalMs = millisSince(prepared.startNanos());
                    return RagChatChunk.done(
                            new Timings(prepared.embeddingMs(), prepared.retrievalMs(), llmMs, totalMs));
                })
                .subscribeOn(Schedulers.boundedElastic());

        return Flux.concat(head, body, tail)
                .onErrorResume(error -> {
                    log.error("RAG generation failed for conversation {}", prepared.conversationId(), error);
                    return Mono.fromCallable(() -> {
                                persistAssistant(prepared, answer.toString());
                                return RagChatChunk.error(
                                        "The assistant failed to complete the answer. Please try again.");
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .flux();
                });
    }

    private void persistAssistant(Prepared prepared, String content) {
        if (StringUtils.hasText(content)) {
            chatMemoryProvider.appendMessage(
                    prepared.conversationId(), prepared.userId(), MessageRole.ASSISTANT, content);
        }
    }

    private List<RetrievedChunk> capContext(List<RetrievedChunk> chunks) {
        List<RetrievedChunk> bounded = new ArrayList<>();
        int budget = ragProperties.maxContextChars();
        int used = 0;
        for (RetrievedChunk chunk : chunks) {
            String content = chunk.content() == null ? "" : chunk.content();
            if (used >= budget) {
                break;
            }
            int remaining = budget - used;
            if (content.length() <= remaining) {
                bounded.add(chunk);
                used += content.length();
            } else {
                bounded.add(new RetrievedChunk(
                        chunk.documentId(), chunk.chunkIndex(), content.substring(0, remaining),
                        chunk.score(), chunk.fileName(), chunk.link()));
                break;
            }
        }
        return bounded;
    }

    private SourceRef toSourceRef(RetrievedChunk chunk) {
        String link = chunk.link() != null ? chunk.link() : ragProperties.linkFor(chunk.documentId());
        return new SourceRef(chunk.documentId(), chunk.fileName(), link,
                chunk.chunkIndex(), chunk.score(), snippet(chunk.content()));
    }

    private String snippet(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.strip().replaceAll("\\s+", " ");
        if (normalized.length() <= SNIPPET_CHARS) {
            return normalized;
        }
        return normalized.substring(0, SNIPPET_CHARS) + "...";
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private record Prepared(
            UUID conversationId,
            String userId,
            List<SourceRef> sources,
            LlmContext context,
            long embeddingMs,
            long retrievalMs,
            long startNanos
    ) {
    }
}
