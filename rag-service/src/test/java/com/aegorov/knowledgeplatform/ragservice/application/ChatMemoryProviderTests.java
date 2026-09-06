package com.aegorov.knowledgeplatform.ragservice.application;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.aegorov.knowledgeplatform.ragservice.TestcontainersConfiguration;
import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationMessage;
import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationSummary;
import com.aegorov.knowledgeplatform.ragservice.persistence.ChatMessageRepository;
import com.aegorov.knowledgeplatform.ragservice.persistence.MessageRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ChatMemoryProviderTests {

    @Autowired
    private ChatMemoryProvider chatMemoryProvider;

    @Autowired
    private ChatMessageRepository messages;

    @Test
    void createsConversationAndAppendsMessagesInSequence() {
        var userId = uniqueUser();
        var conversationId = chatMemoryProvider.createConversation(userId);

        chatMemoryProvider.appendMessage(conversationId, userId, MessageRole.USER, "hello");
        chatMemoryProvider.appendMessage(conversationId, userId, MessageRole.ASSISTANT, "hi");

        var persisted = messages.findByConversationIdOrderBySeqAsc(conversationId);
        assertThat(persisted).extracting(m -> m.getSeq()).containsExactly(1, 2);
        assertThat(persisted).extracting(m -> m.getContent()).containsExactly("hello", "hi");
    }

    @Test
    void listsConversationsMostRecentFirstWithPreview() {
        var userId = uniqueUser();
        var olderConversation = chatMemoryProvider.createConversation(userId);
        chatMemoryProvider.appendMessage(olderConversation, userId, MessageRole.USER, "older question");

        var newerConversation = chatMemoryProvider.createConversation(userId);
        chatMemoryProvider.appendMessage(newerConversation, userId, MessageRole.USER,
                "newer question that should appear first");

        var summaries = chatMemoryProvider.listConversations(userId);
        assertThat(summaries).extracting(ConversationSummary::conversationId)
                .containsExactly(newerConversation, olderConversation);
        assertThat(summaries.get(0).preview()).isEqualTo("newer question that should appear first");
    }

    @Test
    void returnsConversationHistoryInMessageOrder() {
        var userId = uniqueUser();
        var conversationId = chatMemoryProvider.createConversation(userId);
        chatMemoryProvider.appendMessage(conversationId, userId, MessageRole.USER, "one");
        chatMemoryProvider.appendMessage(conversationId, userId, MessageRole.ASSISTANT, "two");

        var response = chatMemoryProvider.getConversationMessages(conversationId, userId);
        assertThat(response.conversationId()).isEqualTo(conversationId);
        assertThat(response.messages()).extracting(ConversationMessage::content).containsExactly("one", "two");
        assertThat(response.messages()).extracting(ConversationMessage::role).containsExactly("USER", "ASSISTANT");
    }

    @Test
    void rejectsConversationAccessFromOtherUser() {
        var owner = uniqueUser();
        var other = uniqueUser();
        var conversationId = chatMemoryProvider.createConversation(owner);

        assertThatThrownBy(() -> chatMemoryProvider.getConversationMessages(conversationId, other))
                .isInstanceOf(ConversationAccessDeniedException.class);
    }

    private static String uniqueUser() {
        return "user-" + UUID.randomUUID();
    }
}
