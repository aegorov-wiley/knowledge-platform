package com.aegorov.knowledgeplatform.documentservice.application.service;

import com.aegorov.knowledgeplatform.documentservice.application.exception.DocumentTooLargeException;
import com.aegorov.knowledgeplatform.documentservice.application.exception.UnsupportedDocumentContentTypeException;
import com.aegorov.knowledgeplatform.documentservice.config.DocumentUploadProperties;
import com.aegorov.knowledgeplatform.documentservice.utils.FileValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.BiFunction;

@Slf4j
@RequiredArgsConstructor
@Component
public class FileValidationServiceImpl implements FileValidationService<MultipartFile, DocumentUploadProperties, FileValidationResult> {

    private final DocumentUploadProperties properties;

    @Override
    public void validate(BiFunction<MultipartFile, DocumentUploadProperties, FileValidationResult> validator,
                         MultipartFile file) {
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
}
