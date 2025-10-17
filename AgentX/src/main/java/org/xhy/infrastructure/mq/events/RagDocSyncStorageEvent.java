package org.xhy.infrastructure.mq.events;

import org.xhy.infrastructure.mq.core.MessageRoute;

/** Route constants for vector storage messages. */
public final class RagDocSyncStorageEvent {

    private RagDocSyncStorageEvent() {
    }

    public static final String EXCHANGE_NAME = "rag.doc.task.syncStorage.exchange10";
    public static final String QUEUE_NAME = "rag.doc.task.syncStorage.queue10";
    public static final String ROUTE_KEY = "rag.doc.task.syncStorage10";

    public static MessageRoute route() {
        return MessageRoute.topic(EXCHANGE_NAME, ROUTE_KEY, QUEUE_NAME);
    }
}
