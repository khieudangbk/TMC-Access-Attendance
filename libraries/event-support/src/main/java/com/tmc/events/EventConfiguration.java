package com.tmc.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableScheduling
@ConditionalOnProperty(name = "tmc.events.enabled", havingValue = "true")
public class EventConfiguration {
    public static final String EXCHANGE = "tmc.events";
    public static final String ATTENDANCE_QUEUE = "tmc.attendance";
    public static final String AUDIT_QUEUE = "tmc.audit";

    @Bean
    Declarables eventTopology() {
        var declarations = new ArrayList<Declarable>();
        declarations.add(new TopicExchange(EXCHANGE, true, false));
        declarations.add(new DirectExchange("tmc.dead", true, false));
        for (String queue : List.of(ATTENDANCE_QUEUE, AUDIT_QUEUE)) {
            declarations.add(new Queue(queue, true, false, false, Map.of(
                    "x-queue-type", "quorum", "x-delivery-limit", 5,
                    "x-overflow", "reject-publish", "x-dead-letter-strategy", "at-least-once",
                    "x-dead-letter-exchange", "tmc.dead", "x-dead-letter-routing-key", queue)));
            declarations.add(QueueBuilder.durable(queue + ".dead").quorum().withArgument("x-delivery-limit", -1).build());
            declarations.add(new Binding(queue + ".dead", Binding.DestinationType.QUEUE, "tmc.dead", queue, null));
        }
        declarations.add(new Binding(ATTENDANCE_QUEUE, Binding.DestinationType.QUEUE, EXCHANGE,
                "access.passage_confirmed", null));
        for (String routingKey : List.of("access.#", "audit.#", "monitor.#")) {
            declarations.add(new Binding(AUDIT_QUEUE, Binding.DestinationType.QUEUE, EXCHANGE, routingKey, null));
        }
        return new Declarables(declarations);
    }
}
