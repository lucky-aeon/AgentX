package org.xhy.infrastructure.mq.utils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;
import org.xhy.infrastructure.mq.model.MQSendEventModel;

import jakarta.annotation.Resource;

/**
 * @author zang
 * @date 14:03 <br/>
 */
public class RabbitMQUtils {

    private final Logger logger = LoggerFactory.getLogger(RabbitMQUtils.class);

    
    private static final Map<String, Queue> QUEUE_MAP = new ConcurrentHashMap<>();

    private static final Map<String, Exchange> EXCHANGE_MAP = new ConcurrentHashMap<>();

    @Resource
    private AmqpAdmin amqpAdmin;
    @Resource
    private RabbitTemplate rabbitTemplate;


    /**
     * 发送消息
     */
    public <T> void pushMsg(MQSendEventModel<T> model) {
        createExchangeAndBindQueue(model);
        rabbitTemplate.convertAndSend(model.exchangeName(), model.routeKey(), model.getMsgBody(),
                new CorrelationData(Objects.nonNull(model.getTraceId())
                        ? model.getTraceId() : String.valueOf(System.currentTimeMillis())));
    }

    /**
     * 发送消息:只发送消息的data对象数据
     */
    public <T> void pushMsgData(MQSendEventModel<T> model) {
        createExchangeAndBindQueue(model);
        rabbitTemplate.convertAndSend(model.exchangeName(), model.routeKey(), model.getData(),
                new CorrelationData(Objects.nonNull(model.getTraceId())
                        ? model.getTraceId() : String.valueOf(System.currentTimeMillis())));
    }


    /**
     * 发送消息
     *
     * @param ttlTime 超时时间（单位：毫秒）
     */
    public <T> void pushMsg(MQSendEventModel<T> model, Long ttlTime) {
        createExchangeAndBindQueue(model);
        rabbitTemplate.convertAndSend(model.exchangeName(), model.routeKey(), model.getMsgBody(), m -> {
            m.getMessageProperties().setExpiration(ttlTime.toString());
            return m;
        }, new CorrelationData(Objects.nonNull(model.getTraceId())
                ? model.getTraceId() : String.valueOf(System.currentTimeMillis())));
    }

    /**
     * 创建交换机并绑定队列
     *
     * @param model
     * @param <T>
     */
    public <T> void createExchangeAndBindQueue(MQSendEventModel<T> model) {
        if (!EXCHANGE_MAP.containsKey(model.exchangeName())) {
            // 注册交换机
            Exchange exchange = new CustomExchange(model.exchangeName(), model.exchangeType(), true, false);
            amqpAdmin.declareExchange(exchange);
            EXCHANGE_MAP.put(model.exchangeName(), exchange);
        }
        if (!QUEUE_MAP.containsKey(model.queueName())) {
            //获取队列
            Queue queue = getQueue(model);
            //绑定关系
            Binding binding = new Binding(queue.getName(), Binding.DestinationType.QUEUE, model.exchangeName(),
                    model.routeKey(), null);
            amqpAdmin.declareQueue(queue);
            amqpAdmin.declareBinding(binding);
        }
    }

    private Queue getQueue(String queueName) {
        if (QUEUE_MAP.containsKey(queueName)) {
            return QUEUE_MAP.get(queueName);
        } else {
            Queue queue = new Queue(queueName, true);
            QUEUE_MAP.put(queueName, queue);
            return queue;
        }
    }

    private Queue getQueue(MQSendEventModel model) {
        if (QUEUE_MAP.containsKey(model.queueName())) {
            return QUEUE_MAP.get(model.queueName());
        } else {
            Queue queue = new Queue(model.queueName(), true, false, false, model.arguments());
            QUEUE_MAP.put(model.queueName(), queue);
            return queue;
        }
    }
}
