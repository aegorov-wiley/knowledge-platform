package com.aegorov.knowledgeplatform.ragservice.application;

import java.util.NoSuchElementException;

public class ConversationNotFoundException extends NoSuchElementException {

    public ConversationNotFoundException(String message) {
        super(message);
    }
}
