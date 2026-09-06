package com.aegorov.knowledgeplatform.documentservice.controller.error;

import com.aegorov.knowledgeplatform.documentservice.application.exception.DocumentStorageException;
import com.aegorov.knowledgeplatform.documentservice.application.exception.DocumentTooLargeException;
import com.aegorov.knowledgeplatform.documentservice.application.exception.UnsupportedDocumentContentTypeException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Clock;

@RestControllerAdvice
@RequiredArgsConstructor
public class RestExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(UnsupportedDocumentContentTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedContentType(
            UnsupportedDocumentContentTypeException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({DocumentTooLargeException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiErrorResponse> handleTooLarge(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.CONTENT_TOO_LARGE, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({DocumentStorageException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> handleStorageFailure(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                clock.instant(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        ));
    }
}
