package com.aegorov.knowledgeplatform.documentservice.messaging;

import com.aegorov.knowledgeplatform.documentservice.config.OutboxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisherService {

    private final OutboxJdbcRepository outboxJdbcRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties outboxProperties;
    private final Clock clock;
    @Value("${knowledge-platform.kafka.topics.documents-uploaded:knowledge.documents.v1}")
    private String documentsUploadedTopic;
    private final String workerId = UUID.randomUUID().toString();

    @Scheduled(fixedDelayString = "${knowledge-platform.outbox.publish-delay}")
    public void publishDueEvents() {
        Instant now = clock.instant();
        List<OutboxClaimedEvent> batch = outboxJdbcRepository.claimBatch(
                workerId,
                outboxProperties.batchSize(),
                outboxProperties.claimTimeout(),
                now
        );

        for (OutboxClaimedEvent event : batch) {
            try {
                kafkaTemplate.send(documentsUploadedTopic, event.aggregateId().toString(), event.payload())
                        .get(10, TimeUnit.SECONDS);
                outboxJdbcRepository.markPublished(event.id(), workerId, clock.instant());
            } catch (Exception ex) {
                outboxJdbcRepository.release(event.id(), workerId);
                log.warn("Failed to publish outbox event {}", event.id(), ex);
            }
        }
    }
}
