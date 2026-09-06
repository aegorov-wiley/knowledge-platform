package com.aegorov.knowledgeplatform.indexingservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge-platform.kafka")
public record KafkaConsumerProperties(
        Topics topics,
        Consumer consumer
) {
    public record Topics(String documentsUploaded) {}
    public record Consumer(String groupId) {}
}
