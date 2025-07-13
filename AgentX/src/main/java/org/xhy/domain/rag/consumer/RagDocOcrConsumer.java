package org.xhy.domain.rag.consumer;

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
import org.xhy.domain.rag.constant.FileInitializeStatus;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.service.FileDetailDomainService;
import org.xhy.domain.rag.straegy.RagDocSyncOcrStrategy;
import org.xhy.domain.rag.straegy.context.RagDocSyncOcrContext;
import org.xhy.infrastructure.mq.events.RagDocSyncOcrEvent;
import org.xhy.infrastructure.mq.model.MqMessage;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rabbitmq.client.Channel;

/** OCR预处理消费者
 * @author zang
 * @date 2025-01-10 */
@RabbitListener(bindings = @QueueBinding(value = @Queue(RagDocSyncOcrEvent.QUEUE_NAME), exchange = @Exchange(value = RagDocSyncOcrEvent.EXCHANGE_NAME, type = ExchangeTypes.TOPIC), key = RagDocSyncOcrEvent.ROUTE_KEY))
@Component
public class RagDocOcrConsumer {

    private static final Logger log = LoggerFactory.getLogger(RagDocOcrConsumer.class);

    private final RagDocSyncOcrContext ragDocSyncOcrContext;
    private final FileDetailDomainService fileDetailDomainService;

    public RagDocOcrConsumer(RagDocSyncOcrContext ragDocSyncOcrContext,
            FileDetailDomainService fileDetailDomainService) {
        this.ragDocSyncOcrContext = ragDocSyncOcrContext;
        this.fileDetailDomainService = fileDetailDomainService;
    }

    @RabbitHandler
    public void receiveMessage(Message message, String msg, Channel channel) throws IOException {
        MqMessage mqMessageBody = JSONObject.parseObject(msg, MqMessage.class);

        MDC.put(HEADER_NAME_TRACE_ID,
                Objects.nonNull(mqMessageBody.getTraceId()) ? mqMessageBody.getTraceId() : IdWorker.getTimeId());
        MessageProperties messageProperties = message.getMessageProperties();
        long deliveryTag = messageProperties.getDeliveryTag();
        RagDocSyncOcrMessage ocrMessage = JSON.parseObject(JSON.toJSONString(mqMessageBody.getData()),
                RagDocSyncOcrMessage.class);

        try {
            log.info("Starting OCR processing for file: {}", ocrMessage.getFileId());

            // 更新文件状态为初始化中
            fileDetailDomainService.updateFileInitializeStatus(ocrMessage.getFileId(),
                    FileInitializeStatus.INITIALIZING);
            fileDetailDomainService.updateFileProgress(ocrMessage.getFileId(), 0, 0.0);

            // 获取文件扩展名并选择处理策略
            String fileExt = fileDetailDomainService.getFileExtension(ocrMessage.getFileId());
            if (fileExt == null) {
                throw new RuntimeException("文件扩展名不能为空");
            }

            RagDocSyncOcrStrategy strategy = ragDocSyncOcrContext.getTaskExportStrategy(fileExt.toUpperCase());
            if (strategy == null) {
                throw new RuntimeException("不支持的文件类型: " + fileExt);
            }

            // 执行OCR处理
            strategy.handle(ocrMessage, fileExt.toUpperCase());

            // 完成初始化
            fileDetailDomainService.updateFileInitializeStatus(ocrMessage.getFileId(),
                    FileInitializeStatus.INITIALIZED);

            log.info("OCR processing completed for file: {}", ocrMessage.getFileId());

        } catch (Exception e) {
            log.error("OCR processing failed for file: {}", ocrMessage.getFileId(), e);
            // 处理失败
            fileDetailDomainService.updateFileInitializeStatus(ocrMessage.getFileId(),
                    FileInitializeStatus.INITIALIZATION_FAILED);
            fileDetailDomainService.updateFileProgress(ocrMessage.getFileId(), 0, 0.0);
        } finally {
            channel.basicAck(deliveryTag, false);
        }
    }
}