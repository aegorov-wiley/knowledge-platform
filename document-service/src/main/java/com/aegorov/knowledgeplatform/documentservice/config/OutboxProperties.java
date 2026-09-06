package com.aegorov.knowledgeplatform.documentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "knowledge-platform.outbox")
public record OutboxProperties(
        int batchSize,
        Duration claimTimeout,
        Duration publishDelay
) {
}
