package com.aegorov.knowledgeplatform.ragservice.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {

    List<ChatMessageEntity> findByConversationIdOrderBySeqAsc(UUID conversationId);

    /**
     * Highest {@code seq} currently stored for the conversation, or {@code 0}
     * when the conversation is empty. Used by {@code ChatMemoryProvider} to
     * allocate the next sequence number under the writer transaction.
     */
    @Query("""
            select coalesce(max(m.seq), 0)
            from ChatMessageEntity m
            where m.conversationId = :conversationId
            """)
    int findMaxSeq(@Param("conversationId") UUID conversationId);
}
