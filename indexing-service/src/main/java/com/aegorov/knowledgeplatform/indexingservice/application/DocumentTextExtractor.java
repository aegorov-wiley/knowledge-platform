package com.aegorov.knowledgeplatform.indexingservice.application;

public interface DocumentTextExtractor {

    String extract(byte[] content, String contentType, String fileName);
}
