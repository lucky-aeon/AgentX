package org.xhy.infrastructure.mq.core;

import java.util.Objects;

/**
 * A lightweight route descriptor describing where to publish a message.
 */
public final class MessageRoute {

    private final String exchange;
    private final String routingKey;
    private final String queue;
    private final String type; // e.g. topic, fanout, direct

    private MessageRoute(String exchange, String routingKey, String queue, String type) {
        this.exchange = Objects.requireNonNull(exchange, "exchange");
        this.routingKey = Objects.requireNonNull(routingKey, "routingKey");
        this.queue = Objects.requireNonNull(queue, "queue");
        this.type = Objects.requireNonNull(type, "type");
    }

    public static MessageRoute topic(String exchange, String routingKey, String queue) {
        return new MessageRoute(exchange, routingKey, queue, "topic");
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getQueue() {
        return queue;
    }

    public String getType() {
        return type;
    }
}

