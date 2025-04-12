package org.xhy.infrastructure.mq.model;

import java.io.Serial;
import java.util.List;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.context.ApplicationEvent;
import org.xhy.infrastructure.mq.enums.EventType;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * MQ消息发送事件
 * @author zang
 * @date 14:16 <br/>
 */
public abstract class MQSendEventModel<T> extends ApplicationEvent {

    public static final String HEADER_NAME_TRACE_ID = "seqId";
    @Serial
    private static final long serialVersionUID = -6122931520230435895L;
    private static final String traceId = MDC.get(HEADER_NAME_TRACE_ID);
    private final T data;

    public MQSendEventModel(T data) {
        super(data);
        this.data = data;
    }

    public abstract String description();

    public abstract String exchangeName();

    public abstract String queueName();

    public abstract String routeKey();

    public abstract List<EventType> eventType();

    public String exchangeType() {
        return ExchangeTypes.TOPIC;
    }

    public Map<String, Object> arguments() {
        return null;
    }

    public String getTraceId() {
        return traceId;
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("exchangeName", exchangeName());
        jsonObject.put("routeKey", routeKey());
        jsonObject.put("queueName", queueName());
        jsonObject.put("traceId", traceId);
        jsonObject.put("description", description());
        jsonObject.put("eventTypes", eventType());
        jsonObject.put("msgBody", getMsgBody());
        jsonObject.put("arguments", arguments());
        return jsonObject.toString();
    }

    public String getMsgBody() {
        MqMessage<T> msg = new MqMessage<>();
        msg.setTraceId(traceId);
        msg.setEventTypes(eventType());
        msg.setDescription(description());
        msg.setData(data);
        msg.setTimestamp(System.currentTimeMillis());
        return JSON.toJSONString(msg);
    }

    public String getData() {
        return JSON.toJSONString(data);
    }
}
