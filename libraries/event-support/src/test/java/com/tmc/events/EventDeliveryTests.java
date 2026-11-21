package com.tmc.events;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventDeliveryTests {
    @Test
    void ackFollowsSuccessfulDurableProcessing() throws Exception {
        Channel channel = mock(Channel.class);
        Runnable durable = mock(Runnable.class);
        EventDelivery.consume(message(), channel, durable);
        var sequence = inOrder(durable, channel);
        sequence.verify(durable).run();
        sequence.verify(channel).basicAck(7, false);
    }

    @Test
    void invalidEventIsDeadLetteredAndTransientFailureUsesBoundedBrokerRequeue() throws Exception {
        Channel invalid = mock(Channel.class);
        EventDelivery.consume(message(), invalid, () -> { throw new EventDelivery.InvalidEvent(); });
        verify(invalid).basicReject(7, false);
        verify(invalid, never()).basicAck(anyLong(), anyBoolean());
        Channel unavailable = mock(Channel.class);
        EventDelivery.consume(message(), unavailable, () -> { throw new IllegalStateException(); });
        verify(unavailable).basicReject(7, true);
        verify(unavailable, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void ackConnectionFailurePropagatesSoBrokerCanRedeliver() throws Exception {
        Channel channel = mock(Channel.class);
        doThrow(new java.io.IOException()).when(channel).basicAck(7, false);
        assertThrows(java.io.IOException.class, () -> EventDelivery.consume(message(), channel, () -> {}));
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    private Message message() {
        var properties = new MessageProperties();
        properties.setDeliveryTag(7);
        return new Message(new byte[0], properties);
    }
}
