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
import org.xhy.domain.rag.message.RagDocSyncStorageMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.service.EmbeddingDomainService;
import org.xhy.domain.rag.service.FileDetailDomainService;
import org.xhy.infrastructure.mq.events.RagDocSyncStorageEvent;
import org.xhy.infrastructure.mq.model.MqMessage;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;

/** @author shilong.zang
 * @date 20:51 <br/>
 */
@RabbitListener(bindings = @QueueBinding(value = @Queue(RagDocSyncStorageEvent.QUEUE_NAME), exchange = @Exchange(value = RagDocSyncStorageEvent.EXCHANGE_NAME, type = ExchangeTypes.TOPIC), key = RagDocSyncStorageEvent.ROUTE_KEY))
@Component
public class RagDocStorageConsumer {

    private static final Logger log = LoggerFactory.getLogger(RagDocStorageConsumer.class);

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
    public void receiveMessage(Message message, String msg, Channel channel) throws IOException {
        MqMessage mqMessageBody = JSONObject.parseObject(msg, MqMessage.class);

        MDC.put(HEADER_NAME_TRACE_ID,
                Objects.nonNull(mqMessageBody.getTraceId()) ? mqMessageBody.getTraceId() : IdWorker.getTimeId());
        MessageProperties messageProperties = message.getMessageProperties();
        long deliveryTag = messageProperties.getDeliveryTag();
        RagDocSyncStorageMessage mqRecordReqDTO = JSON.parseObject(JSON.toJSONString(mqMessageBody.getData()),
                RagDocSyncStorageMessage.class);
        try {
            log.info("Current file {} Page {} ———— Starting vectorization", mqRecordReqDTO.getFileName(),
                    mqRecordReqDTO.getPage());

            // 执行向量化处理
            embeddingService.syncStorage(mqRecordReqDTO);

            // 更新向量化进度（假设每个页面向量化完成后更新）
            updateEmbeddingProgress(mqRecordReqDTO);

            log.info("Current file {} Page {} ———— Vectorization finished", mqRecordReqDTO.getFileName(),
                    mqRecordReqDTO.getPage());
        } catch (Exception e) {
            log.error("Exception occurred during vectorization", e);
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
                log.debug("Updated embedding progress for file {}: {}/{} ({}%)", fileId, currentCompletedPages,
                        totalPages, String.format("%.1f", progress));

                // 检查是否所有页面都已完成向量化
                if (currentCompletedPages >= totalPages) {
                    // 确保进度为100%
                    fileDetailDomainService.updateFileEmbeddingProgress(fileId, totalPages, 100.0);
                    // 更新向量化状态为已完成
                    fileDetailDomainService.updateFileEmbeddingStatus(fileId,
                            org.xhy.domain.rag.constant.EmbeddingStatus.INITIALIZED);
                    log.info("All pages vectorized for file {}, marking as completed", fileId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update embedding progress for file {}: {}", message.getFileId(), e.getMessage());
        }
    }

}
