package org.xhy.infrastructure.mq.events;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import org.xhy.infrastructure.mq.enums.EventType;
import org.xhy.infrastructure.mq.model.MQSendEventModel;

/**
 * @author shilong.zang
 * @date 10:21 <br/>
 */
public class DocIeInferEvent<T> extends MQSendEventModel<T> {

    @Serial
    private static final long serialVersionUID = 1896067714032219430L;
    private final EventType[] eventType;
    

    public void setDescription(String description) {
        this.description = description;
    }

    private String description = "文件ocr任务发送成功";

    public DocIeInferEvent(T data, EventType... eventType) {
        super(data);
        this.eventType = eventType;
    }
    //
    public static final String EXCHANGE_NAME = "ie.doc.task.infer.exchange";
    public static final String QUEUE_NAME = "ie.doc.task.infer.queue";
    public static final String ROUTE_KEY = "ie.doc.task.infer";

    @Override
    public String description() {
        return description;
    }

    @Override
    public String exchangeName() {
        return "ie.doc.task.infer.exchange";
    }

    @Override
    public String queueName() {
        return "ie.doc.task.infer.queue";
    }

    @Override
    public String routeKey() {
        return "ie.doc.task.infer";
    }

    @Override
    public List<EventType> eventType() {
        return Arrays.asList(eventType);
    }
}