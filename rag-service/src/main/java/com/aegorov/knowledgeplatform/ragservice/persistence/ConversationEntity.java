package com.aegorov.knowledgeplatform.ragservice.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * A RAG chat conversation owned by exactly one user. The row is created up
 * front (see {@code ChatMemoryProvider.createConversation}) so a caller
 * cannot claim ownership of a UUID that another user allocated.
 */
@Entity
@Table(name = "rag_conversations", schema = "rag_service")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
public class ConversationEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_access_at", nullable = false)
    private Instant lastAccessAt;
}
