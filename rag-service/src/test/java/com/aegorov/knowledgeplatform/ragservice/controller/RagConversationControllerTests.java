package com.aegorov.knowledgeplatform.ragservice.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.aegorov.knowledgeplatform.ragservice.application.ChatMemoryProvider;
import com.aegorov.knowledgeplatform.ragservice.application.ConversationAccessDeniedException;
import com.aegorov.knowledgeplatform.ragservice.application.ConversationNotFoundException;
import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationHistoryResponse;
import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationMessage;
import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationSummary;
import com.aegorov.knowledgeplatform.ragservice.persistence.ChatMessageEntity;
import com.aegorov.knowledgeplatform.ragservice.persistence.MessageRole;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RagConversationControllerTests {

    private static final String USER_ID = "user-123";
    private static final UUID CONVERSATION_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID MESSAGE_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private ChatMemoryProvider chatMemoryProvider;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        chatMemoryProvider = mock(ChatMemoryProvider.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RagConversationController(chatMemoryProvider)).build();
    }

    @Test
    void createConversationReturnsCreatedId() throws Exception {
        when(chatMemoryProvider.createConversation(USER_ID)).thenReturn(CONVERSATION_ID);

        mockMvc.perform(post("/api/v1/rag/conversations")
                        .principal(() -> USER_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId").value(CONVERSATION_ID.toString()));
    }

    @Test
    void appendMessageReturnsPersistedMessage() throws Exception {
        when(chatMemoryProvider.appendMessage(eq(CONVERSATION_ID), eq(USER_ID), eq(MessageRole.USER), eq("hello")))
                .thenReturn(ChatMessageEntity.builder()
                        .id(MESSAGE_ID)
                        .conversationId(CONVERSATION_ID)
                        .seq(1)
                        .role(MessageRole.USER)
                        .content("hello")
                        .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                        .build());

        mockMvc.perform(post("/api/v1/rag/conversations/{conversationId}/messages", CONVERSATION_ID)
                        .principal(() -> USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER","content":"hello"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(MESSAGE_ID.toString()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.content").value("hello"));

        verify(chatMemoryProvider).appendMessage(CONVERSATION_ID, USER_ID, MessageRole.USER, "hello");
    }

    @Test
    void listConversationsReturnsSummaries() throws Exception {
        when(chatMemoryProvider.listConversations(USER_ID)).thenReturn(List.of(
                new ConversationSummary(CONVERSATION_ID, Instant.parse("2026-01-01T00:00:00Z"), "preview")));

        mockMvc.perform(get("/api/v1/rag/conversations")
                        .principal(() -> USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conversationId").value(CONVERSATION_ID.toString()))
                .andExpect(jsonPath("$[0].preview").value("preview"));
    }

    @Test
    void getConversationMessagesReturnsHistory() throws Exception {
        when(chatMemoryProvider.getConversationMessages(CONVERSATION_ID, USER_ID))
                .thenReturn(new ConversationHistoryResponse(CONVERSATION_ID, List.of(
                        new ConversationMessage(MESSAGE_ID, "USER", "hello", Instant.parse("2026-01-01T00:00:00Z")))));

        mockMvc.perform(get("/api/v1/rag/conversations/{conversationId}/messages", CONVERSATION_ID)
                        .principal(() -> USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(CONVERSATION_ID.toString()))
                .andExpect(jsonPath("$.messages[0].id").value(MESSAGE_ID.toString()));
    }

    @Test
    void mapsNotFoundAndForbidden() throws Exception {
        when(chatMemoryProvider.getConversationMessages(CONVERSATION_ID, USER_ID))
                .thenThrow(new ConversationNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/rag/conversations/{conversationId}/messages", CONVERSATION_ID)
                        .principal(() -> USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().string("missing"));

        when(chatMemoryProvider.createConversation(anyString())).thenThrow(new ConversationAccessDeniedException("denied"));
        mockMvc.perform(post("/api/v1/rag/conversations")
                        .principal(() -> USER_ID))
                .andExpect(status().isForbidden())
                .andExpect(content().string("denied"));
    }
}
