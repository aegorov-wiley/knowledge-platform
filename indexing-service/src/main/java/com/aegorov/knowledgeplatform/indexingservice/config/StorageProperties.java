package com.aegorov.knowledgeplatform.indexingservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "knowledge-platform.storage.local")
public record StorageProperties(Path root) {
}
