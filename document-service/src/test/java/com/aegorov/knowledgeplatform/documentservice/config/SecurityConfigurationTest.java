package com.aegorov.knowledgeplatform.documentservice.config;

import com.aegorov.knowledgeplatform.documentservice.application.dto.DocumentUploadResponse;
import com.aegorov.knowledgeplatform.documentservice.application.service.DocumentUploadService;
import com.aegorov.knowledgeplatform.documentservice.controller.DocumentController;
import com.aegorov.knowledgeplatform.documentservice.controller.error.RestExceptionHandler;
import com.aegorov.knowledgeplatform.documentservice.persistence.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc
@Import({SecurityConfiguration.class,
        ClockConfiguration.class,
        RestExceptionHandler.class,
        ServletWebSecurityAutoConfiguration.class})
class SecurityConfigurationTest {

    private static final String DOCUMENTS_PATH = "/api/v1/documents";
    private static final String FILE_PART = "file";
    private static final String FILE_NAME = "doc.txt";
    private static final String USER_NAME = "user";
    private static final String ADMIN_NAME = "admin";
    private static final String ROLE_USER = "USER";
    private static final String ROLE_ADMIN = "ADMIN";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentUploadService documentUploadService;

    private MockMultipartFile file() {
        return new MockMultipartFile(FILE_PART, FILE_NAME, MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes());
    }

    @Test
    void unauthenticatedUploadReturns401() throws Exception {
        mockMvc.perform(multipart(DOCUMENTS_PATH).file(file()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userRoleUploadReturns403() throws Exception {
        mockMvc.perform(multipart(DOCUMENTS_PATH).file(file())
                        .with(user(USER_NAME).roles(ROLE_USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleUploadReturns202() throws Exception {
        var response = new DocumentUploadResponse(
                UUID.randomUUID(), FILE_NAME, DocumentStatus.UPLOADED, Instant.now());
        when(documentUploadService.upload(any())).thenReturn(response);

        mockMvc.perform(multipart(DOCUMENTS_PATH).file(file())
                        .with(user(ADMIN_NAME).roles(ROLE_ADMIN)))
                .andExpect(status().isAccepted());
    }

    @Test
    void basicAuthAdminReturns202() throws Exception {
        var response = new DocumentUploadResponse(
                UUID.randomUUID(), FILE_NAME, DocumentStatus.UPLOADED, Instant.now());
        when(documentUploadService.upload(any())).thenReturn(response);

        mockMvc.perform(multipart(DOCUMENTS_PATH).file(file())
                        .with(httpBasic(ADMIN_NAME, ADMIN_NAME)))
                .andExpect(status().isAccepted());
    }

    @Test
    void basicAuthUserReturns403() throws Exception {
        mockMvc.perform(multipart(DOCUMENTS_PATH).file(file())
                        .with(httpBasic(USER_NAME, USER_NAME)))
                .andExpect(status().isForbidden());
    }

    @Test
    void actuatorHealthPermittedWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound()); // Permitted by security rules (non-401/403)
    }
}
