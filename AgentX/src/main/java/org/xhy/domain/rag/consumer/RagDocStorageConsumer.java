package org.xhy.domain.rag.consumer;

import static org.xhy.infrastructure.mq.core.MessageHeaders.TRACE_ID;

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
import org.xhy.domain.rag.message.RagDocSyncStorageMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.service.EmbeddingDomainService;
import org.xhy.domain.rag.service.FileDetailDomainService;
import org.xhy.infrastructure.mq.core.MessageEnvelope;
import org.xhy.infrastructure.mq.events.RagDocSyncStorageEvent;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;

/** @author shilong.zang
 * @date 20:51 <br/>
 */
@RabbitListener(bindings = @QueueBinding(value = @Queue(RagDocSyncStorageEvent.QUEUE_NAME), exchange = @Exchange(value = RagDocSyncStorageEvent.EXCHANGE_NAME, type = ExchangeTypes.TOPIC), key = RagDocSyncStorageEvent.ROUTE_KEY))
@Component
public class RagDocStorageConsumer {

    private static final Logger log = LoggerFactory.getLogger(RagDocStorageConsumer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final EmbeddingDomainService embeddingService;
    private final FileDetailDomainService fileDetailDomainService;
    private final DocumentUnitRepository documentUnitRepository;

    public RagDocStorageConsumer(EmbeddingDomainService embeddingService,
            FileDetailDomainService fileDetailDomainService, DocumentUnitRepository documentUnitRepository) {
        this.embeddingService = embeddingService;
        this.fileDetailDomainService = fileDetailDomainService;
        this.documentUnitRepository = documentUnitRepository;
    }

    @RabbitHandler
    public void receiveMessage(java.util.Map<String, Object> payload, Message message, Channel channel)
            throws IOException {
        MessageProperties messageProperties = message.getMessageProperties();
        long deliveryTag = messageProperties.getDeliveryTag();

        try {
            // 将已由 Jackson 转换的 Map 转为强类型 Envelope
            MessageEnvelope<RagDocSyncStorageMessage> envelope = OBJECT_MAPPER.convertValue(payload,
                    new TypeReference<MessageEnvelope<RagDocSyncStorageMessage>>() {
                    });

            MDC.put(TRACE_ID, Objects.nonNull(envelope.getTraceId()) ? envelope.getTraceId() : IdWorker.getTimeId());
            RagDocSyncStorageMessage mqRecordReqDTO = envelope.getData();

            log.info("当前文件 {} 页面 {} ———— 开始向量化", mqRecordReqDTO.getFileName(), mqRecordReqDTO.getPage());

            // 执行向量化处理
            embeddingService.syncStorage(mqRecordReqDTO);

            // 更新向量化进度（假设每个页面向量化完成后更新）
            updateEmbeddingProgress(mqRecordReqDTO);

            log.info("当前文件 {} 第{}页 ———— 向量化完成", mqRecordReqDTO.getFileName(), mqRecordReqDTO.getPage());
        } catch (Exception e) {
            log.error("向量化过程中发生异常", e);
        } finally {
            channel.basicAck(deliveryTag, false);
        }
    }

    /** 更新向量化进度
     * @param message 向量化消息 */
    private void updateEmbeddingProgress(RagDocSyncStorageMessage message) {
        try {
            String fileId = message.getFileId();
            Integer pageIndex = message.getPage(); // 这是从0开始的页面索引

            // 获取文件总页数来计算进度
            var fileEntity = fileDetailDomainService.getFileByIdWithoutUserCheck(fileId);
            Integer totalPages = fileEntity.getFilePageSize();

            if (totalPages != null && totalPages > 0) {
                // 查询已完成向量化的页面数量
                long completedVectorPages = documentUnitRepository
                        .selectCount(Wrappers.<DocumentUnitEntity>lambdaQuery()
                                .eq(DocumentUnitEntity::getFileId, fileId).eq(DocumentUnitEntity::getIsVector, true));

                // 当前页面完成后的总完成页数
                int currentCompletedPages = (int) (completedVectorPages + 1);

                // 计算百分比：已完成的页数 / 总页数 * 100
                double progress = ((double) currentCompletedPages / totalPages) * 100.0;

                fileDetailDomainService.updateFileEmbeddingProgress(fileId, currentCompletedPages, progress);
                log.debug("更新文件{}的嵌入进度: {}/{} ({}%)", fileId, currentCompletedPages, totalPages,
                        String.format("%.1f", progress));

                // 检查是否所有页面都已完成向量化
                if (currentCompletedPages >= totalPages) {
                    // 确保进度为100%
                    fileDetailDomainService.updateFileEmbeddingProgress(fileId, totalPages, 100.0);
                    // 通过状态机完成向量化处理
                    fileDetailDomainService.completeFileEmbeddingProcessing(fileId, fileEntity.getUserId());
                    log.info("文件{}的所有页面均已向量化，标记为完成", fileId);
                }
            }
        } catch (Exception e) {
            log.warn("更新文件{}的嵌入进度失败: {}", message.getFileId(), e.getMessage());
        }
    }

}
