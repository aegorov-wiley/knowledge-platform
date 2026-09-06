package com.aegorov.knowledgeplatform;

import org.springframework.boot.SpringApplication;

public class TestKnowledgePlatformApplication {

    static void main(String[] args) {
        SpringApplication.from(KnowledgePlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
