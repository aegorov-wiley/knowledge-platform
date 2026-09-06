package com.aegorov.knowledgeplatform.ragservice.persistence;

/**
 * Role of a persisted chat message. The value is stored as a VARCHAR with a
 * DB {@code CHECK} constraint that mirrors this enum, so any drift between
 * code and schema fails fast on write.
 */
public enum MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
