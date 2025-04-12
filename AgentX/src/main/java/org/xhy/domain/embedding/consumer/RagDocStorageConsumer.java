package org.xhy.domain.embedding.consumer;

import static org.xhy.infrastructure.mq.model.MQSendEventModel.HEADER_NAME_TRACE_ID;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.xhy.domain.embedding.RagDocSyncStorageMessage;
import org.xhy.domain.embedding.service.EmbeddingService;
import org.xhy.infrastructure.mq.events.RagDocSyncStorageEvent;
import org.xhy.infrastructure.mq.model.MqMessage;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rabbitmq.client.Channel;

/**
 * @author shilong.zang
 * @date 20:51 <br/>
 */
@RabbitListener(bindings = @QueueBinding(value = @Queue(RagDocSyncStorageEvent.QUEUE_NAME),
        exchange = @Exchange(value = RagDocSyncStorageEvent.EXCHANGE_NAME , type = ExchangeTypes.TOPIC),
        key = RagDocSyncStorageEvent.ROUTE_KEY))
@Component
public class RagDocStorageConsumer {

    private static final Logger log = LoggerFactory.getLogger(RagDocStorageConsumer.class);

    private final EmbeddingService embeddingService;


    public RagDocStorageConsumer(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @RabbitHandler
    public void receiveMessage(Message message, String msg, Channel channel) throws IOException {
        MqMessage mqMessageBody = JSONObject.parseObject(msg, MqMessage.class);

        MDC.put(HEADER_NAME_TRACE_ID, Objects.nonNull(mqMessageBody.getTraceId())
                ? mqMessageBody.getTraceId() : IdWorker.getTimeId());
        MessageProperties messageProperties = message.getMessageProperties();
        long deliveryTag = messageProperties.getDeliveryTag();
        RagDocSyncStorageMessage mqRecordReqDTO = JSON.parseObject(JSON.toJSONString(mqMessageBody.getData()),
                RagDocSyncStorageMessage.class);
        try {
            log.info("当前文件{}第{}页 ————  向量化开始",mqRecordReqDTO.getFileName(),mqRecordReqDTO.getPage());
            embeddingService.syncStorage(mqRecordReqDTO);
            log.info("当前文件{}第{}页 ————  向量化结束",mqRecordReqDTO.getFileName(),mqRecordReqDTO.getPage());
        } catch (Exception e) {
            log.error("向量化发生异常", e);
        } finally {
            channel.basicAck(deliveryTag, false);
        }
    }

}
