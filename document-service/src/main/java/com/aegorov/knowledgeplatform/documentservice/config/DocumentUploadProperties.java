package com.aegorov.knowledgeplatform.documentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "knowledge-platform.documents")
public record DocumentUploadProperties(DataSize maxSize) {
}
