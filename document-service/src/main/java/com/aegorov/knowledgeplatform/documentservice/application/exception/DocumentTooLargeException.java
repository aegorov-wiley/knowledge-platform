package com.aegorov.knowledgeplatform.documentservice.application.exception;

public class DocumentTooLargeException extends RuntimeException {

    public DocumentTooLargeException(String message) {
        super(message);
    }
}
