package com.aegorov.knowledgeplatform.ragservice.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.aegorov.knowledgeplatform.ragservice.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the R2 persistence layer against a real Postgres container:
 * migrations create the tables, JPA maps them correctly, ownership queries
 * work, cascade-delete removes messages with the parent conversation, and
 * the DB-side role CHECK constraint rejects unknown roles.
 * <p>
 * Every test allocates a fresh user id (UUID) so state leaked by
 * non-transactional tests cannot influence its assertions.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RagPersistenceTests {

    @Autowired
    private ConversationRepository conversations;

    @Autowired
    private ChatMessageRepository messages;

    @Autowired
    private DataSource dataSource;

    @Test
    @Transactional
    void savesAndLoadsConversationsByUser() {
        var alice = "alice-" + UUID.randomUUID();
        var other = "bob-" + UUID.randomUUID();
        var now = Instant.parse("2026-01-01T00:00:00Z");
        var alice1 = conversations.save(newConversation(alice, now));
        var alice2 = conversations.save(newConversation(alice, now.plusSeconds(60)));
        conversations.save(newConversation(other, now));

        var forAlice = conversations.findByUserIdOrderByLastAccessAtDesc(alice);
        assertThat(forAlice).extracting(ConversationEntity::getId)
                .containsExactly(alice2.getId(), alice1.getId());
    }

    @Test
    @Transactional
    void findMaxSeqReturnsZeroForEmptyConversation() {
        var conv = conversations.save(newConversation(uniqueUser(), Instant.now()));

        assertThat(messages.findMaxSeq(conv.getId())).isZero();
    }

    @Test
    @Transactional
    void findsMessagesOrderedBySequence() {
        var conv = conversations.save(newConversation(uniqueUser(), Instant.now()));

        messages.save(newMessage(conv.getId(), 2, MessageRole.ASSISTANT, "b"));
        messages.save(newMessage(conv.getId(), 1, MessageRole.USER, "a"));

        assertThat(messages.findByConversationIdOrderBySeqAsc(conv.getId()))
                .extracting(ChatMessageEntity::getContent)
                .containsExactly("a", "b");
        assertThat(messages.findMaxSeq(conv.getId())).isEqualTo(2);
    }

    @Test
    void cascadeDeleteRemovesMessagesWithConversation() {
        var conv = conversations.save(newConversation(uniqueUser(), Instant.now()));
        messages.save(newMessage(conv.getId(), 1, MessageRole.USER, "hi"));

        conversations.deleteById(conv.getId());

        assertThat(messages.findByConversationIdOrderBySeqAsc(conv.getId())).isEmpty();
    }

    @Test
    void rejectsUnknownRoleAtDatabaseLevel() {
        var conv = conversations.save(newConversation(uniqueUser(), Instant.now()));
        var jdbc = JdbcClient.create(dataSource);
        var messageId = UUID.randomUUID();
        var createdAt = OffsetDateTime.now(ZoneOffset.UTC);

        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO rag_service.rag_messages
                            (id, conversation_id, seq, role, content, created_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """)
                .params(messageId, conv.getId(), 1, "BOGUS", "x", createdAt)
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);

        // Clean up the parent so this test leaves no residual rows.
        conversations.deleteById(conv.getId());
    }

    private static String uniqueUser() {
        return "user-" + UUID.randomUUID();
    }

    private static ConversationEntity newConversation(String userId, Instant when) {
        return ConversationEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .createdAt(when)
                .lastAccessAt(when)
                .build();
    }

    private static ChatMessageEntity newMessage(UUID conversationId, int seq, MessageRole role, String content) {
        return ChatMessageEntity.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .seq(seq)
                .role(role)
                .content(content)
                .createdAt(Instant.now())
                .build();
    }
}

