package com.aegorov.knowledgeplatform.documentservice.utils;

import com.aegorov.knowledgeplatform.documentservice.config.DocumentUploadProperties;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.function.BiFunction;

import static com.aegorov.knowledgeplatform.documentservice.application.service.DocumentUploadService.SUPPORTED_CONTENT_TYPES;
import static com.aegorov.knowledgeplatform.documentservice.utils.FileValidationResult.*;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public interface FileValidationUtil extends BiFunction<MultipartFile, DocumentUploadProperties, FileValidationResult> {

    static FileValidationUtil isFilePresent() {
        return (file, props) -> isNull(file) || file.isEmpty()
                ? NO_FILE_PROVIDED : SUCCESS;
    }
    static FileValidationUtil isContentTypePresent() {
        return (file, props) -> isNull(file.getContentType()) || file.getContentType().isBlank()
                ? NO_CONTENT_TYPE_PROVIDED : SUCCESS;
    }
    static FileValidationUtil isContentTypeSupported() {
        return (file, props) -> {
            var normalizedContentType = Optional.of(file)
                    .map(MultipartFile::getContentType)
                    .map(String::toLowerCase).orElse(EMPTY);
            return !SUPPORTED_CONTENT_TYPES.contains(normalizedContentType)
                    ? NO_SUPPORTED_CONTENT_TYPE_PROVIDED : SUCCESS;
        };
    }

    static FileValidationUtil isFileNamePresent() {
        return (file, props) -> isNull(file.getOriginalFilename())
                ? NO_FILE_PROVIDED : SUCCESS;
    }

    static FileValidationUtil isFileSizeCorrect() {
        return (file, props) -> file.getSize() > props.maxSize().toBytes()
                ? MAX_FILE_SIZE_EXCEEDED : SUCCESS;
    }

    default FileValidationUtil and(FileValidationUtil other) {
        return (file, props) -> {
            var result = apply(file, props);
            return result.equals(SUCCESS) ? other.apply(file, props) : result;
        };
    }

}
