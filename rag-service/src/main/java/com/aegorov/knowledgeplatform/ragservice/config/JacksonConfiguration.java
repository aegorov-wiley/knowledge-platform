package com.aegorov.knowledgeplatform.ragservice.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a classic (fasterxml) {@link ObjectMapper} bean, matching
 * document-service and indexing-service. Spring Boot 4 auto-configures a Jackson 3
 * ({@code tools.jackson}) mapper for WebFlux codecs; rag-service serializes NDJSON
 * chat chunks by hand and needs the fasterxml mapper explicitly.
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .findAndRegisterModules();
    }
}
