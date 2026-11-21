package com.tmc.accessevent.messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "tmc.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final String EXCHANGE = "tmc.events";
    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMandatory(true);
    }

    @Scheduled(fixedDelayString = "${tmc.outbox.poll-interval-ms:1000}")
    @Transactional(rollbackFor = Exception.class)
    public void publishPending() throws Exception {
        var events = jdbcTemplate.query("""
                SELECT event_id, event_type, payload::text FROM outbox_events
                WHERE published_at IS NULL ORDER BY created_at LIMIT 50 FOR UPDATE SKIP LOCKED
                """, (row, rowNumber) -> new PendingEvent(row.getObject("event_id", UUID.class),
                        row.getString("event_type"), row.getString("payload")));
        for (PendingEvent event : events) {
            var properties = new MessageProperties();
            properties.setMessageId(event.eventId().toString());
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            var correlation = new CorrelationData(event.eventId().toString());
            rabbitTemplate.send(EXCHANGE, "access." + event.eventType().toLowerCase(java.util.Locale.ROOT),
                    new Message(event.payload().getBytes(StandardCharsets.UTF_8), properties), correlation);
            var confirmation = correlation.getFuture().get(2, TimeUnit.SECONDS);
            if (!confirmation.ack() || correlation.getReturned() != null) {
                throw new IllegalStateException("Outbox event was not delivered to a queue");
            }
            jdbcTemplate.update("UPDATE outbox_events SET published_at = CURRENT_TIMESTAMP WHERE event_id = ?",
                    event.eventId());
        }
    }

    private record PendingEvent(UUID eventId, String eventType, String payload) {
    }

    @Configuration
    @EnableScheduling
    static class MessagingConfig {
        @Bean
        TopicExchange accessEventExchange() {
            return new TopicExchange(EXCHANGE, true, false);
        }
    }
}
