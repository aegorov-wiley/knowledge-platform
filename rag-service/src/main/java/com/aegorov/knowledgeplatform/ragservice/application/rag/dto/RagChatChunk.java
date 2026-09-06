package com.aegorov.knowledgeplatform.ragservice.application.rag.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One line of the NDJSON chat stream. Exactly one field group is populated per
 * chunk type; unused fields are omitted from JSON.
 * <ul>
 *   <li>{@code META}    — first, carries the conversationId.</li>
 *   <li>{@code SOURCES} — retrieved source attributions (possibly empty).</li>
 *   <li>{@code DELTA}   — an answer token fragment.</li>
 *   <li>{@code DONE}    — terminal success, carries timings.</li>
 *   <li>{@code ERROR}   — terminal failure, carries a user-safe message.</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagChatChunk(
        String type,
        UUID conversationId,
        List<SourceRef> sources,
        String content,
        Timings timings
) {

    public static RagChatChunk meta(UUID conversationId) {
        return new RagChatChunk("META", conversationId, null, null, null);
    }

    public static RagChatChunk sources(List<SourceRef> sources) {
        return new RagChatChunk("SOURCES", null, sources, null, null);
    }

    public static RagChatChunk delta(String content) {
        return new RagChatChunk("DELTA", null, null, content, null);
    }

    public static RagChatChunk done(Timings timings) {
        return new RagChatChunk("DONE", null, null, null, timings);
    }

    public static RagChatChunk error(String message) {
        return new RagChatChunk("ERROR", null, null, message, null);
    }
}
