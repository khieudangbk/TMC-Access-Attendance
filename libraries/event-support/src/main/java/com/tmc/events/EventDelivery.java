package com.tmc.events;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

public final class EventDelivery {
    private EventDelivery() {
    }

    public static void consume(Message message, Channel channel, Runnable durableProcessing) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            durableProcessing.run();
        } catch (InvalidEvent exception) {
            channel.basicReject(deliveryTag, false);
            return;
        } catch (RuntimeException exception) {
            // Quorum delivery-limit bounds transient requeues; evidence remains in the durable DLQ.
            channel.basicReject(deliveryTag, true);
            return;
        }
        // A lost ACK causes replay; durable consumers must deduplicate before this boundary.
        channel.basicAck(deliveryTag, false);
    }

    public static class InvalidEvent extends RuntimeException {
        public InvalidEvent() {
            super("INVALID_EVENT");
        }
    }
}
