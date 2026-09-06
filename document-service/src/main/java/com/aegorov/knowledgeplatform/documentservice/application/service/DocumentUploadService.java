package com.aegorov.knowledgeplatform.documentservice.application.service;

import com.aegorov.knowledgeplatform.documentservice.application.dto.DocumentUploadResponse;
import com.aegorov.knowledgeplatform.documentservice.application.exception.DocumentStorageException;
import com.aegorov.knowledgeplatform.documentservice.application.exception.DocumentTooLargeException;
import com.aegorov.knowledgeplatform.documentservice.application.exception.UnsupportedDocumentContentTypeException;
import com.aegorov.knowledgeplatform.documentservice.application.port.FileStorage;
import com.aegorov.knowledgeplatform.documentservice.config.DocumentUploadProperties;
import com.aegorov.knowledgeplatform.documentservice.utils.FileValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import static com.aegorov.knowledgeplatform.documentservice.utils.FileValidationUtil.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentUploadService
        implements FileValidationService<MultipartFile, DocumentUploadProperties, FileValidationResult> {

    public static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/markdown",
            "text/plain"
    );

    private final FileStorage fileStorage;
    private final DocumentPersistenceService documentPersistenceService;
    private final DocumentUploadProperties uploadProperties;

    public DocumentUploadResponse upload(MultipartFile file) {

        validate(isFilePresent()
                .and(isContentTypePresent())
                .and(isContentTypeSupported())
                .and(isFileNamePresent())
                .and(isFileSizeCorrect()), file, uploadProperties);

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

    @Override
    public void validate(BiFunction<MultipartFile, DocumentUploadProperties, FileValidationResult> validator,
                         MultipartFile file, DocumentUploadProperties properties) {
        var result = validator.apply(file, properties);
        switch (result) {
            case SUCCESS -> log.debug("Document validated successfully");
            case NO_FILE_PROVIDED -> throw new IllegalArgumentException("Uploaded file is required");
            case NO_CONTENT_TYPE_PROVIDED -> throw new IllegalArgumentException("Content type is required");
            case NO_SUPPORTED_CONTENT_TYPE_PROVIDED ->
                    throw new UnsupportedDocumentContentTypeException("Unsupported content type '%s'"
                            .formatted(file.getContentType()));
            case MAX_FILE_SIZE_EXCEEDED -> throw new DocumentTooLargeException(
                    "Uploaded file exceeds the maximum allowed size of '%s'".formatted(properties.maxSize()));
        }
    }
    private String storageKey(UUID documentId, String fileName) {
        return "documents/" + documentId + "/" + normalizeFileName(fileName);
    }

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
