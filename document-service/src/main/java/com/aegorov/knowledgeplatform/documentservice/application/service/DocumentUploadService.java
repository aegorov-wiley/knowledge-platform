package com.aegorov.knowledgeplatform.documentservice.application.service;

import com.aegorov.knowledgeplatform.documentservice.application.dto.DocumentUploadResponse;
import com.aegorov.knowledgeplatform.documentservice.application.exception.DocumentStorageException;
import com.aegorov.knowledgeplatform.documentservice.application.port.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import static com.aegorov.knowledgeplatform.documentservice.utils.FileValidationUtil.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentUploadService {

    public static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/markdown",
            "text/plain"
    );

    private final FileStorage fileStorage;
    private final DocumentPersistenceService documentPersistenceService;
    private final FileValidationServiceImpl fileValidationService;

    public DocumentUploadResponse upload(MultipartFile file) {

        fileValidationService.validate(isFilePresent()
                .and(isContentTypePresent())
                .and(isContentTypeSupported())
                .and(isFileNamePresent())
                .and(isFileSizeCorrect()), file);

        var fileName = file.getOriginalFilename();
        var contentType = file.getContentType();
        var uploadTarget = documentPersistenceService.resolveUploadTarget(fileName);
        var storageKey = uploadTarget.storageKey() != null
                ? uploadTarget.storageKey()
                : storageKey(uploadTarget.documentId(), fileName);

        try (InputStream inputStream = file.getInputStream()) {
            fileStorage.store(storageKey, inputStream, file.getSize(), contentType);
        } catch (IOException ex) {
            throw new DocumentStorageException("Failed to store uploaded file", ex);
        }

        try {
            return documentPersistenceService.createDocument(
                    uploadTarget.documentId(),
                    fileName,
                    contentType,
                    file.getSize(),
                    storageKey
            );
        } catch (RuntimeException ex) {
            cleanupStoredFile(storageKey, ex);
            throw ex;
        }
    }

    // Constructs the storage path key for a document.
    // Example: (123e4567-e89b-12d3-a456-426614174000, "my resume.pdf") -> "documents/123e4567-e89b-12d3-a456-426614174000/my_resume.pdf"
    private String storageKey(UUID documentId, String fileName) {
        return "documents/" + documentId + "/" + normalizeFileName(fileName);
    }

    // Sanitizes file names by replacing spaces, slashes, and invalid characters with underscores.
    // Example: "hello world/file!.pdf" -> "hello_world_file_.pdf"
    private String normalizeFileName(String fileName) {
        var normalized = fileName.replaceAll("[\\\\/\\s]+", "_").replaceAll("[^A-Za-z0-9._-]", "_");
        return normalized.isBlank() ? "document" : normalized;
    }

    private void cleanupStoredFile(String storageKey, RuntimeException originalException) {
        try {
            fileStorage.delete(storageKey);
        } catch (IOException cleanupEx) {
            originalException.addSuppressed(cleanupEx);
        }
    }
}
