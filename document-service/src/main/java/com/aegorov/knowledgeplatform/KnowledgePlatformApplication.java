package com.aegorov.knowledgeplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableRetry
public class KnowledgePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgePlatformApplication.class, args);
    }

}
