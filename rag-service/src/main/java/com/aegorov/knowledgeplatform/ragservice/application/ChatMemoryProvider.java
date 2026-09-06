package com.aegorov.knowledgeplatform.ragservice.application;

import java.util.UUID;
import java.util.List;

import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationHistoryResponse;
import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationSummary;
import com.aegorov.knowledgeplatform.ragservice.persistence.ChatMessageEntity;
import com.aegorov.knowledgeplatform.ragservice.persistence.MessageRole;

public interface ChatMemoryProvider {

    UUID createConversation(String userId);

    ChatMessageEntity appendMessage(UUID conversationId, String userId, MessageRole role, String content);

    ConversationHistoryResponse getConversationMessages(UUID conversationId, String userId);

    List<ConversationSummary> listConversations(String userId);
}
