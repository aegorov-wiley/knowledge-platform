package com.aegorov.knowledgeplatform.ragservice.application.llm;

import java.util.List;

import com.aegorov.knowledgeplatform.ragservice.application.retrieval.RetrievedChunk;

/**
 * Everything the LLM needs to answer one question: system instructions, the
 * retrieved context chunks, prior conversation turns, and the new question.
 *
 * @param systemPrompt grounding / behavioural instructions for the model.
 * @param history      prior turns (oldest first), excluding the new question.
 * @param chunks       retrieved context chunks, most relevant first.
 * @param question     the user's new question.
 */
public record LlmContext(
        String systemPrompt,
        List<Turn> history,
        List<RetrievedChunk> chunks,
        String question
) {

    /**
     * A prior conversation turn.
     *
     * @param role    USER / ASSISTANT / SYSTEM.
     * @param content message text.
     */
    public record Turn(String role, String content) {
    }
}
