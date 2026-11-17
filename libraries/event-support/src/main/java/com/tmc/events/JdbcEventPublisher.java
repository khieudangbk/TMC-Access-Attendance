package com.tmc.events;

import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@ConditionalOnProperty(name = "tmc.events.enabled", havingValue = "true")
public class JdbcEventPublisher {
    private final JdbcTemplate database;
    private final RabbitTemplate rabbit;

    public JdbcEventPublisher(JdbcTemplate database, RabbitTemplate rabbit) {
        this.database = database;
        this.rabbit = rabbit;
    }

    @Scheduled(fixedDelayString = "${tmc.events.poll-interval-ms:1000}")
    @Transactional(rollbackFor = Exception.class)
    public void publish() throws Exception {
        var pending = database.query("""
                SELECT event_id, routing_key, payload::text FROM integration_outbox
                WHERE published_at IS NULL ORDER BY created_at, event_id LIMIT 50 FOR UPDATE SKIP LOCKED
                """, (row, index) -> new Pending(row.getObject(1, UUID.class), row.getString(2), row.getString(3)));
        for (Pending event : pending) {
            ConfirmedPublisher.send(rabbit, event.eventId().toString(), event.routingKey(), event.payload());
            database.update("UPDATE integration_outbox SET published_at = CURRENT_TIMESTAMP WHERE event_id = ?", event.eventId());
        }
    }

    private record Pending(UUID eventId, String routingKey, String payload) {
    }
}
