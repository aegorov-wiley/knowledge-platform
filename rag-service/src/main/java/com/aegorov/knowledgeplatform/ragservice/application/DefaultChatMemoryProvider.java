package com.aegorov.knowledgeplatform.ragservice.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationHistoryResponse;
import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationMessage;
import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationSummary;
import com.aegorov.knowledgeplatform.ragservice.persistence.ChatMessageEntity;
import com.aegorov.knowledgeplatform.ragservice.persistence.ChatMessageRepository;
import com.aegorov.knowledgeplatform.ragservice.persistence.ConversationEntity;
import com.aegorov.knowledgeplatform.ragservice.persistence.ConversationRepository;
import com.aegorov.knowledgeplatform.ragservice.persistence.MessageRole;

@Service
@Transactional
public class DefaultChatMemoryProvider implements ChatMemoryProvider {

    private static final int MAX_PREVIEW_LENGTH = 120;

    private final ConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final Clock clock;

    public DefaultChatMemoryProvider(ConversationRepository conversations,
                                     ChatMessageRepository messages,
                                     Clock clock) {
        this.conversations = conversations;
        this.messages = messages;
        this.clock = clock;
    }

    @Override
    public UUID createConversation(String userId) {
        requireUserId(userId);
        Instant now = clock.instant();
        var conversation = ConversationEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .createdAt(now)
                .lastAccessAt(now)
                .build();
        return conversations.save(conversation).getId();
    }

    @Override
    public ChatMessageEntity appendMessage(UUID conversationId, String userId, MessageRole role, String content) {
        requireUserId(userId);
        requireConversationId(conversationId);
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Message content is required");
        }

        loadOwnedConversation(conversationId, userId);
        int nextSeq = messages.findMaxSeq(conversationId) + 1;
        Instant now = clock.instant();
        var message = ChatMessageEntity.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .seq(nextSeq)
                .role(role)
                .content(content)
                .createdAt(now)
                .build();
        conversations.touchLastAccess(conversationId, now);
        return messages.save(message);
    }

    @Override
    public ConversationHistoryResponse getConversationMessages(UUID conversationId, String userId) {
        requireUserId(userId);
        requireConversationId(conversationId);
        loadOwnedConversation(conversationId, userId);

        var orderedMessages = messages.findByConversationIdOrderBySeqAsc(conversationId)
                .stream()
                .map(message -> new ConversationMessage(
                        message.getId(),
                        message.getRole().name(),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();
        return new ConversationHistoryResponse(conversationId, orderedMessages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummary> listConversations(String userId) {
        requireUserId(userId);
        return conversations.findByUserIdOrderByLastAccessAtDesc(userId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private ConversationSummary toSummary(ConversationEntity conversation) {
        var preview = messages.findByConversationIdOrderBySeqAsc(conversation.getId())
                .stream()
                .filter(message -> message.getRole() == MessageRole.USER)
                .map(ChatMessageEntity::getContent)
                .findFirst()
                .map(this::truncatePreview)
                .orElse(null);
        return new ConversationSummary(conversation.getId(), conversation.getLastAccessAt(), preview);
    }

    private ConversationEntity loadOwnedConversation(UUID conversationId, String userId) {
        var conversation = conversations.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(
                        "Conversation '%s' was not found".formatted(conversationId)));
        if (!userId.equals(conversation.getUserId())) {
            throw new ConversationAccessDeniedException(
                    "Conversation '%s' does not belong to user '%s'".formatted(conversationId, userId));
        }
        return conversation;
    }

    private static void requireUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("User id is required");
        }
    }

    private static void requireConversationId(UUID conversationId) {
        if (conversationId == null) {
            throw new IllegalArgumentException("Conversation id is required");
        }
    }

    private String truncatePreview(String content) {
        if (content.length() <= MAX_PREVIEW_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_PREVIEW_LENGTH - 3) + "...";
    }
}
