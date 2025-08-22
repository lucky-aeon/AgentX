package org.xhy.domain.rag.consumer;

import static org.xhy.infrastructure.mq.model.MQSendEventModel.HEADER_NAME_TRACE_ID;

import java.io.IOException;
import java.util.List;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.message.RagDocSyncStorageMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.service.FileDetailDomainService;
import org.xhy.domain.rag.strategy.DocumentProcessingStrategy;
import org.xhy.domain.rag.strategy.context.DocumentProcessingContext;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.mq.enums.EventType;
import org.xhy.infrastructure.mq.events.RagDocSyncOcrEvent;
import org.xhy.infrastructure.mq.events.RagDocSyncStorageEvent;
import org.xhy.infrastructure.mq.model.MqMessage;
import org.xhy.infrastructure.rag.service.UserModelConfigResolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;

/** OCR预处理消费者
 * @author zang
 * @date 2025-01-10 */
@RabbitListener(bindings = @QueueBinding(value = @Queue(RagDocSyncOcrEvent.QUEUE_NAME), exchange = @Exchange(value = RagDocSyncOcrEvent.EXCHANGE_NAME, type = ExchangeTypes.TOPIC), key = RagDocSyncOcrEvent.ROUTE_KEY))
@Component
public class RagDocOcrConsumer {

    private static final Logger log = LoggerFactory.getLogger(RagDocOcrConsumer.class);

    private final DocumentProcessingContext ragDocSyncOcrContext;
    private final FileDetailDomainService fileDetailDomainService;
    private final DocumentUnitRepository documentUnitRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserModelConfigResolver userModelConfigResolver;

    public RagDocOcrConsumer(DocumentProcessingContext ragDocSyncOcrContext, FileDetailDomainService fileDetailDomainService,
                             DocumentUnitRepository documentUnitRepository, ApplicationEventPublisher applicationEventPublisher,
                             UserModelConfigResolver userModelConfigResolver) {
        this.ragDocSyncOcrContext = ragDocSyncOcrContext;
        this.fileDetailDomainService = fileDetailDomainService;
        this.documentUnitRepository = documentUnitRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.userModelConfigResolver = userModelConfigResolver;
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

            // 获取文件并开始OCR处理
            FileDetailEntity fileEntity = fileDetailDomainService.getFileByIdWithoutUserCheck(ocrMessage.getFileId());
            boolean startSuccess = fileDetailDomainService.startFileOcrProcessing(ocrMessage.getFileId(),
                    fileEntity.getUserId());

            if (!startSuccess) {
                throw new BusinessException("无法开始OCR处理，文件状态不允许");
            }

            // 获取文件扩展名并选择处理策略
            String fileExt = fileDetailDomainService.getFileExtension(ocrMessage.getFileId());
            if (fileExt == null) {
                throw new BusinessException("文件扩展名不能为空");
            }

            DocumentProcessingStrategy strategy = ragDocSyncOcrContext.getDocumentStrategyHandler(fileExt.toUpperCase());
            if (strategy == null) {
                throw new BusinessException("不支持的文件类型: " + fileExt);
            }

            // 执行OCR处理
            strategy.handle(ocrMessage, fileExt.toUpperCase());

            // 完成OCR处理
            fileEntity = fileDetailDomainService.getFileByIdWithoutUserCheck(ocrMessage.getFileId());
            Integer totalPages = fileEntity.getFilePageSize();
            if (totalPages != null && totalPages > 0) {
                fileDetailDomainService.updateFileOcrProgress(ocrMessage.getFileId(), totalPages, totalPages,
                        fileEntity.getUserId());
            }

            boolean completeSuccess = fileDetailDomainService.completeFileOcrProcessing(ocrMessage.getFileId(),
                    fileEntity.getUserId());
            if (!completeSuccess) {
                log.warn("OCR处理完成但状态转换失败，文件ID: {}", ocrMessage.getFileId());
            }

            log.info("OCR processing completed for file: {}", ocrMessage.getFileId());

            // 自动启动向量化处理
            autoStartVectorization(ocrMessage.getFileId(), fileEntity);

        } catch (Exception e) {
            log.error("OCR processing failed for file: {}", ocrMessage.getFileId(), e);
            // 处理失败
            try {
                FileDetailEntity fileEntity = fileDetailDomainService
                        .getFileByIdWithoutUserCheck(ocrMessage.getFileId());
                fileDetailDomainService.failFileOcrProcessing(ocrMessage.getFileId(), fileEntity.getUserId());
            } catch (Exception ex) {
                log.error("Failed to update file status to failed for file: {}", ocrMessage.getFileId(), ex);
            }
        } finally {
            channel.basicAck(deliveryTag, false);
        }
    }

    /** 自动启动向量化处理
     * @param fileId 文件ID
     * @param fileEntity 文件实体 */
    private void autoStartVectorization(String fileId, FileDetailEntity fileEntity) {
        try {
            log.info("Auto-starting vectorization for file: {}", fileId);

            // 检查是否有可用的文档单元进行向量化
            List<DocumentUnitEntity> documentUnits = documentUnitRepository
                    .selectList(Wrappers.lambdaQuery(DocumentUnitEntity.class).eq(DocumentUnitEntity::getFileId, fileId)
                            .eq(DocumentUnitEntity::getIsOcr, true).eq(DocumentUnitEntity::getIsVector, false));

            if (documentUnits.isEmpty()) {
                log.warn("No document units found for vectorization for file: {}", fileId);
                return;
            }

            // 更新向量化状态
            boolean startSuccess = fileDetailDomainService.startFileEmbeddingProcessing(fileId, fileEntity.getUserId());
            if (!startSuccess) {
                log.warn("无法开始向量化处理，文件状态不允许，文件ID: {}", fileId);
                return;
            }

            // 为每个DocumentUnit发送向量化MQ消息
            for (DocumentUnitEntity documentUnit : documentUnits) {
                RagDocSyncStorageMessage storageMessage = new RagDocSyncStorageMessage();
                storageMessage.setId(documentUnit.getId());
                storageMessage.setFileId(fileId);
                storageMessage.setFileName(fileEntity.getOriginalFilename());
                storageMessage.setPage(documentUnit.getPage());
                storageMessage.setContent(documentUnit.getContent());
                storageMessage.setVector(true);
                storageMessage.setDatasetId(fileEntity.getDataSetId());
                storageMessage.setUserId(fileEntity.getUserId());
                // 获取用户的嵌入模型配置
                storageMessage.setEmbeddingModelConfig(
                        userModelConfigResolver.getUserEmbeddingModelConfig(fileEntity.getUserId()));

                RagDocSyncStorageEvent<RagDocSyncStorageMessage> storageEvent = new RagDocSyncStorageEvent<>(
                        storageMessage, EventType.DOC_SYNC_RAG);
                storageEvent.setDescription("文件自动向量化处理任务 - 页面 " + documentUnit.getPage());
                applicationEventPublisher.publishEvent(storageEvent);
            }

            log.info("Auto-vectorization started for file: {}, {} document units", fileId, documentUnits.size());

        } catch (Exception e) {
            log.error("Failed to auto-start vectorization for file: {}", fileId, e);
            // 如果自动启动失败，重置向量化状态
            try {
                fileDetailDomainService.failFileEmbeddingProcessing(fileId, fileEntity.getUserId());
            } catch (Exception ex) {
                log.error("Failed to update file embedding status to failed for file: {}", fileId, ex);
            }
        }
    }
}