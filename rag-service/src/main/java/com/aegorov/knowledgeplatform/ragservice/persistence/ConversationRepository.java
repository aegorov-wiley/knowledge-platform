package com.aegorov.knowledgeplatform.ragservice.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    List<ConversationEntity> findByUserIdOrderByLastAccessAtDesc(String userId);

    Optional<ConversationEntity> findByIdAndUserId(UUID id, String userId);

    @Modifying
    @Query("update ConversationEntity c set c.lastAccessAt = :timestamp where c.id = :id")
    int touchLastAccess(@Param("id") UUID id, @Param("timestamp") Instant timestamp);
}
