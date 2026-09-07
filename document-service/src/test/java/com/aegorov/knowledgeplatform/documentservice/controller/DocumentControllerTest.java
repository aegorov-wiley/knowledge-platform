package com.aegorov.knowledgeplatform.documentservice.controller;

import com.aegorov.knowledgeplatform.documentservice.application.dto.DocumentUploadResponse;
import com.aegorov.knowledgeplatform.documentservice.application.exception.DocumentStorageException;
import com.aegorov.knowledgeplatform.documentservice.application.exception.DocumentTooLargeException;
import com.aegorov.knowledgeplatform.documentservice.application.exception.UnsupportedDocumentContentTypeException;
import com.aegorov.knowledgeplatform.documentservice.application.service.DocumentUploadService;
import com.aegorov.knowledgeplatform.documentservice.config.ClockConfiguration;
import com.aegorov.knowledgeplatform.documentservice.controller.error.RestExceptionHandler;
import com.aegorov.knowledgeplatform.documentservice.persistence.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ClockConfiguration.class, RestExceptionHandler.class})
class DocumentControllerTest {

    private static final String PATH = "/api/v1/documents";
    private static final String FILE_PART = "file";
    private static final String FILE_NAME = "doc.txt";

    private static final String JSON_STATUS = "$.status";
    private static final String JSON_ERROR = "$.error";
    private static final String JSON_MESSAGE = "$.message";
    private static final String JSON_PATH = "$.path";

    private static final String MSG_BAD_TYPE = "bad type";
    private static final String MSG_TOO_BIG = "too big";
    private static final String MSG_DISK_FULL = "disk full";
    private static final String MSG_BAD_STATE = "bad state";
    private static final String MSG_BAD_ARG = "bad arg";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentUploadService documentUploadService;

    private MockMultipartFile file() {
        return new MockMultipartFile(FILE_PART, FILE_NAME, MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes());
    }

    @Test
    void uploadReturnsSuccessStatus() throws Exception {
        var response = new DocumentUploadResponse(
                UUID.randomUUID(), FILE_NAME, DocumentStatus.UPLOADED, Instant.now());
        when(documentUploadService.upload(any())).thenReturn(response);

        mockMvc.perform(multipart(PATH).file(file())).andExpect(status().isAccepted());
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        when(documentUploadService.upload(any()))
                .thenThrow(new UnsupportedDocumentContentTypeException(MSG_BAD_TYPE));

        mockMvc.perform(multipart(PATH).file(file()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath(JSON_STATUS).value(415))
                .andExpect(jsonPath(JSON_ERROR).value("Unsupported Media Type"))
                .andExpect(jsonPath(JSON_MESSAGE).value(MSG_BAD_TYPE))
                .andExpect(jsonPath(JSON_PATH).value(PATH));
    }

    @Test
    void documentTooLargeReturns413() throws Exception {
        when(documentUploadService.upload(any()))
                .thenThrow(new DocumentTooLargeException(MSG_TOO_BIG));

        mockMvc.perform(multipart(PATH).file(file()))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath(JSON_STATUS).value(413))
                .andExpect(jsonPath(JSON_MESSAGE).value(MSG_TOO_BIG))
                .andExpect(jsonPath(JSON_PATH).value(PATH));
    }

    @Test
    void maxUploadSizeExceededReturns413() throws Exception {
        when(documentUploadService.upload(any()))
                .thenThrow(new MaxUploadSizeExceededException(10));

        mockMvc.perform(multipart(PATH).file(file()))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath(JSON_STATUS).value(413))
                .andExpect(jsonPath(JSON_PATH).value(PATH));
    }

    @Test
    void storageFailureReturns500() throws Exception {
        when(documentUploadService.upload(any()))
                .thenThrow(new DocumentStorageException(MSG_DISK_FULL, new RuntimeException()));

        mockMvc.perform(multipart(PATH).file(file()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(JSON_STATUS).value(500))
                .andExpect(jsonPath(JSON_MESSAGE).value(MSG_DISK_FULL))
                .andExpect(jsonPath(JSON_PATH).value(PATH));
    }

    @Test
    void illegalStateReturns500() throws Exception {
        when(documentUploadService.upload(any()))
                .thenThrow(new IllegalStateException(MSG_BAD_STATE));

        mockMvc.perform(multipart(PATH).file(file()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(JSON_STATUS).value(500));
    }

    @Test
    void illegalArgumentReturns400() throws Exception {
        when(documentUploadService.upload(any()))
                .thenThrow(new IllegalArgumentException(MSG_BAD_ARG));

        mockMvc.perform(multipart(PATH).file(file()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_STATUS).value(400))
                .andExpect(jsonPath(JSON_MESSAGE).value(MSG_BAD_ARG))
                .andExpect(jsonPath(JSON_PATH).value(PATH));
    }

    @Test
    void missingFilePartReturns400() throws Exception {
        mockMvc.perform(multipart(PATH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_STATUS).value(400))
                .andExpect(jsonPath(JSON_PATH).value(PATH));
    }
}
