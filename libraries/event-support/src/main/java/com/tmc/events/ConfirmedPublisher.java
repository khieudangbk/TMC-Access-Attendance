package com.tmc.events;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public final class ConfirmedPublisher {
    private ConfirmedPublisher() {
    }

    public static void send(RabbitTemplate template, String eventId, String routingKey, String payload) throws Exception {
        var properties = new MessageProperties();
        properties.setMessageId(eventId);
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        var correlation = new CorrelationData(eventId);
        template.setMandatory(true);
        template.send(EventConfiguration.EXCHANGE, routingKey,
                new Message(payload.getBytes(StandardCharsets.UTF_8), properties), correlation);
        var confirmation = correlation.getFuture().get(3, TimeUnit.SECONDS);
        if (!confirmation.ack() || correlation.getReturned() != null) {
            throw new IllegalStateException("EVENT_NOT_ROUTED_OR_CONFIRMED");
        }
    }
}
