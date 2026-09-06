package com.aegorov.knowledgeplatform.indexingservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge-platform.chunking")
public record ChunkingProperties(
        int maxChars,
        int overlapChars
) {
}
