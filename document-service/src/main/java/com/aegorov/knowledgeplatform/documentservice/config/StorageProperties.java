package com.aegorov.knowledgeplatform.documentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "knowledge-platform.storage.local")
public record StorageProperties(Path root) {
}
