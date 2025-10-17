package org.xhy.infrastructure.mq.events;

import org.xhy.infrastructure.mq.core.MessageRoute;

/**
 * Route constants for OCR processing messages.
 */
public final class RagDocSyncOcrEvent {

    private RagDocSyncOcrEvent() {}

    public static final String EXCHANGE_NAME = "rag.doc.task.syncOcr.exchange-10";
    public static final String QUEUE_NAME = "rag.doc.task.syncOcr.queue-10";
    public static final String ROUTE_KEY = "rag.doc.task.syncOcr-10";

    public static MessageRoute route() {
        return MessageRoute.topic(EXCHANGE_NAME, ROUTE_KEY, QUEUE_NAME);
    }
}
