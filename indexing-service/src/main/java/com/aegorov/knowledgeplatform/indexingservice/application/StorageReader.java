package com.aegorov.knowledgeplatform.indexingservice.application;

import com.aegorov.knowledgeplatform.indexingservice.config.StorageProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class StorageReader {

    private final Path root;

    public StorageReader(StorageProperties storageProperties) {
        this.root = storageProperties.root().toAbsolutePath().normalize();
    }

    public byte[] read(String storageKey) {
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read stored document " + storageKey, ex);
        }
    }
}
