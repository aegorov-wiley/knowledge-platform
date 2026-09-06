package com.aegorov.knowledgeplatform.ragservice.controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aegorov.knowledgeplatform.ragservice.application.ChatMemoryProvider;
import com.aegorov.knowledgeplatform.ragservice.application.ConversationAccessDeniedException;
import com.aegorov.knowledgeplatform.ragservice.application.ConversationNotFoundException;
import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationHistoryResponse;
import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationMessage;
import com.aegorov.knowledgeplatform.ragservice.application.dto.ConversationSummary;
import com.aegorov.knowledgeplatform.ragservice.persistence.MessageRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/rag/conversations", produces = MediaType.APPLICATION_JSON_VALUE)
public class RagConversationController {

    private final ChatMemoryProvider chatMemoryProvider;

    @PostMapping
    public ResponseEntity<CreateConversationResponse> createConversation(Principal principal) {
        var conversationId = chatMemoryProvider.createConversation(userId(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateConversationResponse(conversationId));
    }

    @GetMapping
    public ResponseEntity<List<ConversationSummary>> listConversations(Principal principal) {
        return ResponseEntity.ok(chatMemoryProvider.listConversations(userId(principal)));
    }

    @PostMapping(value = "/{conversationId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConversationMessage> appendMessage(
            @PathVariable UUID conversationId,
            Principal principal,
            @Valid @RequestBody AppendMessageRequest request) {
        var message = chatMemoryProvider.appendMessage(conversationId, userId(principal), request.role(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ConversationMessage(message.getId(), message.getRole().name(), message.getContent(), message.getCreatedAt()));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ConversationHistoryResponse> getConversationMessages(
            @PathVariable UUID conversationId,
            Principal principal) {
        return ResponseEntity.ok(chatMemoryProvider.getConversationMessages(conversationId, userId(principal)));
    }

    private static String userId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ConversationAccessDeniedException("Authentication is required");
        }
        return principal.getName();
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<String> notFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(ConversationAccessDeniedException.class)
    public ResponseEntity<String> forbidden(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    public record CreateConversationResponse(UUID conversationId) {
    }

    public record AppendMessageRequest(
            @NotNull MessageRole role,
            @NotBlank String content
    ) {
    }
}
