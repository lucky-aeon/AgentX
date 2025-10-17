package org.xhy.infrastructure.mq.core;

/** Publisher abstraction for outbound messages. */
public interface MessagePublisher {

    void publish(MessageRoute route, MessageEnvelope<?> envelope);

    default void publish(MessageRoute route, MessageEnvelope<?> envelope, Long ttlMillis) {
        publish(route, envelope); // TTL optional; implementations may override
    }
}
