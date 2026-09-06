package com.aegorov.knowledgeplatform.indexingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IndexingServiceApplication {

     static void main(String[] args) {
        SpringApplication.run(IndexingServiceApplication.class, args);
    }
}
