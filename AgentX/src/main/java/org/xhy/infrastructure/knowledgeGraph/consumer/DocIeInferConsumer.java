package org.xhy.infrastructure.knowledgeGraph.consumer;

import static org.xhy.infrastructure.mq.model.MQSendEventModel.HEADER_NAME_TRACE_ID;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rabbitmq.client.Channel;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
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
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.application.knowledgeGraph.service.GraphIngestionAppService;
import org.xhy.application.knowledgeGraph.service.PagedGraphProcessingOrchestrator;
import org.xhy.application.knowledgeGraph.service.ContextAwareGraphExtractionService;
import org.xhy.domain.knowledgeGraph.message.DocIeInferMessage;
import org.xhy.domain.neo4j.constant.GraphExtractorPrompt;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;
import org.xhy.infrastructure.mq.events.DocIeInferEvent;
import org.xhy.infrastructure.mq.model.MqMessage;

/** 文档信息抽取推理消费者 使用LangChain4j从文档文本中提取知识图谱数据
 * 
 * @author shilong.zang
 * @date 15:13 <br/>
 */
@Component
public class DocIeInferConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocIeInferConsumer.class);

    // AI服务和图数据存储服务
    private final GraphIngestionAppService graphIngestionAppService;
    private final PagedGraphProcessingOrchestrator processingOrchestrator;
    private final ContextAwareGraphExtractionService contextAwareExtractionService;

    public DocIeInferConsumer(GraphIngestionAppService graphIngestionAppService,
            PagedGraphProcessingOrchestrator processingOrchestrator,
            ContextAwareGraphExtractionService contextAwareExtractionService) {
        this.graphIngestionAppService = graphIngestionAppService;
        this.processingOrchestrator = processingOrchestrator;
        this.contextAwareExtractionService = contextAwareExtractionService;
    }

    /** 处理文档信息抽取推理事件 */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(DocIeInferEvent.QUEUE_NAME), exchange = @Exchange(value = DocIeInferEvent.EXCHANGE_NAME, type = ExchangeTypes.TOPIC), key = DocIeInferEvent.ROUTE_KEY))
    public void receiveMessage(Message message, String msg, Channel channel) throws IOException {

        log.info("DocIeInferConsumer 收到消息: {}", msg);

        MqMessage mqMessageBody = JSONObject.parseObject(msg, MqMessage.class);

        MDC.put(HEADER_NAME_TRACE_ID,
                Objects.nonNull(mqMessageBody.getTraceId()) ? mqMessageBody.getTraceId() : IdWorker.getTimeId());
        MessageProperties messageProperties = message.getMessageProperties();
        long deliveryTag = messageProperties.getDeliveryTag();
        DocIeInferMessage ocrMessage = JSON.parseObject(JSON.toJSONString(mqMessageBody.getData()),
                DocIeInferMessage.class);

        // 检查重试次数，避免无限重试
        Integer retryCount = (Integer) messageProperties.getHeaders().get("x-delivery-count");
        if (retryCount == null) {
            retryCount = 0;
        }
        final int MAX_RETRY_COUNT = 3;

        if (retryCount >= MAX_RETRY_COUNT) {
            log.error("文档 {} 处理失败次数已达到最大重试次数 {}，消息将被丢弃", ocrMessage.getFileId(), MAX_RETRY_COUNT);
            channel.basicAck(deliveryTag, false);
            MDC.remove(HEADER_NAME_TRACE_ID);
            return;
        }

        if (ocrMessage.isPaged()) {
            log.info("开始处理文档 {} 的知识图谱提取任务，页码: {}/{}, 重试次数: {}", ocrMessage.getFileId(), ocrMessage.getPageNumber(),
                    ocrMessage.getTotalPages(), retryCount);
        } else {
            log.info("开始处理文档 {} 的知识图谱提取任务（非分页模式），重试次数: {}", ocrMessage.getFileId(), retryCount);
        }

        // 标记消息是否已经被处理（确认或拒绝）
        boolean messageProcessed = false;

        try {
            // 验证分页消息的完整性
            if (!processingOrchestrator.validatePagedMessage(ocrMessage)) {
                log.error("分页消息验证失败，跳过处理");
                channel.basicAck(deliveryTag, false);
                messageProcessed = true;
                return;
            }

            // 使用上下文感知的图谱提取服务
            GraphIngestionRequest extractedGraph = contextAwareExtractionService.extractWithContext(ocrMessage);

            if (extractedGraph != null && extractedGraph.getEntities() != null
                    && !extractedGraph.getEntities().isEmpty()) {

                // 使用协调器处理图谱数据（支持分页和非分页模式）
                var response = processingOrchestrator.orchestrateGraphProcessing(ocrMessage, extractedGraph);

                if (response.isSuccess()) {
                    if (ocrMessage.isPaged()) {
                        log.info("文档 {} 第 {} 页知识图谱处理成功，实体数: {}, 关系数: {}", ocrMessage.getFileId(),
                                ocrMessage.getPageNumber(), extractedGraph.getEntities().size(),
                                extractedGraph.getRelationships() != null
                                        ? extractedGraph.getRelationships().size()
                                        : 0);
                    } else {
                        log.info("文档 {} 知识图谱处理成功，实体数: {}, 关系数: {}", ocrMessage.getFileId(),
                                extractedGraph.getEntities().size(),
                                extractedGraph.getRelationships() != null
                                        ? extractedGraph.getRelationships().size()
                                        : 0);
                    }
                } else {
                    log.error("文档 {} 知识图谱处理失败: {}", ocrMessage.getFileId(), response.getMessage());
                    // 处理失败时抛出异常，触发重试逻辑
                    throw new RuntimeException("知识图谱处理失败: " + response.getMessage());
                }
            } else {
                log.warn("未能从文档 {} 中提取到有效的知识图谱数据", ocrMessage.getFileId());
            }

            // 成功处理完成，确认消息
            channel.basicAck(deliveryTag, false);
            messageProcessed = true;

            if (ocrMessage.isPaged()) {
                log.info("文档 {} 第 {} 页处理完成，消息已确认", ocrMessage.getFileId(), ocrMessage.getPageNumber());
            } else {
                log.info("文档 {} 处理完成，消息已确认", ocrMessage.getFileId());
            }

        } catch (Exception e) {
            log.error("处理文档 {} 的知识图谱提取任务失败: {}", ocrMessage.getFileId(), e.getMessage(), e);

            if (!messageProcessed) {
                try {
                    // 如果还没有达到最大重试次数，拒绝消息并重新入队
                    if (retryCount < MAX_RETRY_COUNT - 1) {
                        channel.basicNack(deliveryTag, false, true);
                        messageProcessed = true;

                        if (ocrMessage.isPaged()) {
                            log.info("文档 {} 第 {} 页处理失败，消息已拒绝并重新入队，重试次数: {}/{}", ocrMessage.getFileId(),
                                    ocrMessage.getPageNumber(), retryCount + 1, MAX_RETRY_COUNT);
                        } else {
                            log.info("文档 {} 处理失败，消息已拒绝并重新入队，重试次数: {}/{}", ocrMessage.getFileId(), retryCount + 1,
                                    MAX_RETRY_COUNT);
                        }
                    } else {
                        // 达到最大重试次数，确认消息避免重复处理
                        channel.basicAck(deliveryTag, false);
                        messageProcessed = true;

                        log.error("文档 {} 处理失败且已达到最大重试次数，消息将被丢弃", ocrMessage.getFileId());
                    }
                } catch (IOException ioException) {
                    log.error("消息处理失败: {}", ioException.getMessage(), ioException);
                    // 如果操作失败，尝试确认消息以避免重复消费
                    if (!messageProcessed) {
                        try {
                            channel.basicAck(deliveryTag, false);
                            messageProcessed = true;
                            log.warn("消息操作失败，已强制确认消息以避免重复消费");
                        } catch (IOException ackException) {
                            log.error("强制确认消息也失败: {}", ackException.getMessage(), ackException);
                        }
                    }
                }
            }
        } finally {
            // 清理MDC上下文
            MDC.remove(HEADER_NAME_TRACE_ID);
        }
    }
}
