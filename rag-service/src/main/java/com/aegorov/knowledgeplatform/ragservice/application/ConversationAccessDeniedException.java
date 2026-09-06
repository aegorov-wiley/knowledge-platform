package com.aegorov.knowledgeplatform.ragservice.application;

public class ConversationAccessDeniedException extends RuntimeException {

    public ConversationAccessDeniedException(String message) {
        super(message);
    }
}
