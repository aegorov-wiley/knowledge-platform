package com.aegorov.knowledgeplatform.documentservice.messaging;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class OutboxJdbcRepository {

    private static final RowMapper<OutboxClaimedEvent> ROW_MAPPER = new OutboxClaimedEventRowMapper();

    private final JdbcClient jdbcClient;

    public OutboxJdbcRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional
    public List<OutboxClaimedEvent> claimBatch(String workerId, int batchSize, Duration claimTimeout, Instant now) {
        List<OutboxClaimedEvent> events = jdbcClient.sql("""
                        select id, aggregate_id, event_type, payload
                        from outbox_events
                        where published_at is null
                          and (claimed_until is null or claimed_until < ?)
                        order by created_at
                        for update skip locked
                        limit ?
                        """)
                .param(Timestamp.from(now))
                .param(batchSize)
                .query(ROW_MAPPER)
                .list();

        if (events.isEmpty()) {
            return events;
        }

        Instant claimedUntil = now.plus(claimTimeout);
        for (OutboxClaimedEvent event : events) {
            jdbcClient.sql("""
                            update outbox_events
                            set claimed_by = ?, claimed_until = ?, attempts = attempts + 1
                            where id = ?
                            """)
                    .param(workerId)
                    .param(Timestamp.from(claimedUntil))
                    .param(event.id())
                    .update();
        }

        return events;
    }

    public void markPublished(UUID eventId, String workerId, Instant publishedAt) {
        jdbcClient.sql("""
                        update outbox_events
                        set published_at = ?, claimed_by = null, claimed_until = null
                        where id = ? and claimed_by = ?
                        """)
                .param(Timestamp.from(publishedAt))
                .param(eventId)
                .param(workerId)
                .update();
    }

    public void release(UUID eventId, String workerId) {
        jdbcClient.sql("""
                        update outbox_events
                        set claimed_by = null, claimed_until = null
                        where id = ? and claimed_by = ? and published_at is null
                        """)
                .param(eventId)
                .param(workerId)
                .update();
    }

    private static final class OutboxClaimedEventRowMapper implements RowMapper<OutboxClaimedEvent> {

        @Override
        public OutboxClaimedEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new OutboxClaimedEvent(
                    rs.getObject("id", UUID.class),
                    rs.getObject("aggregate_id", UUID.class),
                    rs.getString("event_type"),
                    rs.getString("payload")
            );
        }
    }
}
