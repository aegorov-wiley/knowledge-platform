package com.aegorov.knowledgeplatform.documentservice.storage;

import com.aegorov.knowledgeplatform.documentservice.application.port.FileStorage;
import com.aegorov.knowledgeplatform.documentservice.config.StorageProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local file storage implementation - make sure to use shared volume in docker env
 */
@Service
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(StorageProperties storageProperties) {
        this.root = storageProperties.root().toAbsolutePath().normalize();
    }

    @Override
    public void store(String storageKey, InputStream content,
                      long contentLength, String contentType) throws IOException {
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            content.transferTo(outputStream);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    private Path resolve(String storageKey) {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return target;
    }
}
