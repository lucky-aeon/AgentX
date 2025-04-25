package org.xhy.infrastructure.mq.events;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;

import org.xhy.infrastructure.mq.enums.EventType;
import org.xhy.infrastructure.mq.model.MQSendEventModel;


/**
 * @author zang
 */
public class RagDocSyncStorageEvent<T> extends MQSendEventModel<T> {

    @Serial
    private static final long serialVersionUID = -8799365828172646170L;
    private final EventType[] eventType;

    public void setDescription(String description) {
        this.description = description;
    }

    private String description = "文件入库任务发送成功";

    public RagDocSyncStorageEvent(T data, EventType... eventType) {
        super(data);
        this.eventType = eventType;
    }
//
    public static final String EXCHANGE_NAME = "rag.doc.task.syncStorage.exchange";
    public static final String QUEUE_NAME = "rag.doc.task.syncStorage.queue";
    public static final String ROUTE_KEY = "rag.doc.task.syncStorage";

    @Override
    public String description() {
        return description;
    }

    @Override
    public String exchangeName() {
        return "rag.doc.task.syncStorage.exchange";
    }

    @Override
    public String queueName() {
        return "rag.doc.task.syncStorage.queue";
    }

    @Override
    public String routeKey() {
        return "rag.doc.task.syncStorage";
    }

    @Override
    public List<EventType> eventType() {
        return Arrays.asList(eventType);
    }
}
