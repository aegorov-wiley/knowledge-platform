package com.aegorov.knowledgeplatform.ragservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param provider LLM provider: {@code stub} (default, offline) or {@code azure} (placeholder).
 */
@ConfigurationProperties(prefix = "knowledge-platform.llm")
public record LlmProperties(
        String provider
) {
}
