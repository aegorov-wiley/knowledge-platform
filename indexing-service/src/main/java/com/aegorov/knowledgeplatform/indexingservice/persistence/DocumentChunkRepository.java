package com.aegorov.knowledgeplatform.indexingservice.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, UUID> {

    @Modifying
    @Query("delete from DocumentChunkEntity c where c.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);
}
