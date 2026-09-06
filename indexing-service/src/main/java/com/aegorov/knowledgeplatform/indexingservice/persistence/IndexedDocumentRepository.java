package com.aegorov.knowledgeplatform.indexingservice.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IndexedDocumentRepository extends JpaRepository<IndexedDocumentEntity, UUID> {
    boolean existsByLastEventId(String lastEventId);
}
