package org.xhy.domain.knowledgeGraph;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.application.knowledgeGraph.service.GraphIngestionService;
import org.xhy.domain.neo4j.constant.GraphExtractorPrompt;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;
import org.xhy.infrastructure.mq.events.DocIeInferEvent;
/**
 * 文档信息抽取推理消费者
 * 使用LangChain4j从文档文本中提取知识图谱数据
 * 
 * @author shilong.zang
 * @date 15:13 <br/>
 */
@RabbitListener(bindings = @QueueBinding(value = @Queue(DocIeInferEvent.QUEUE_NAME), exchange = @Exchange(value = DocIeInferEvent.EXCHANGE_NAME, type = ExchangeTypes.TOPIC), key = DocIeInferEvent.ROUTE_KEY))
@Component
public class DocIeInferConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocIeInferConsumer.class);

    // 滑动窗口配置
    private static final int WINDOW_SIZE = 3;        // 窗口大小
    private static final int OVERLAP_SIZE = 1;       // 重叠页数
    private static final int STEP_SIZE = WINDOW_SIZE - OVERLAP_SIZE; // 窗口移动步长

    // AI服务和图数据存储服务
    private final GraphIngestionService graphIngestionService;

    public DocIeInferConsumer( GraphIngestionService graphIngestionService) {
        this.graphIngestionService = graphIngestionService;
    }

    /**
     * 处理文档信息抽取推理事件
     * 
     * @param event 文档信息抽取事件
     */
    @RabbitListener
    public void processDocumentInference(DocIeInferEvent event) {


        try {
            // 使用AI服务从文本中提取知识图谱
            String documentText = """
                    
                    """;
            final String format = StrUtil.format(GraphExtractorPrompt.graphExtractorPrompt, documentText);
            final UserMessage userMessage = UserMessage.userMessage(
                    TextContent.from(format));

            /** 创建OCR处理的模型配置 - 从消息中获取用户配置的OCR模型 */
            ProviderConfig ocrProviderConfig = new ProviderConfig(
                    "", "https://api.siliconflow.cn/v1",
                    "Qwen/Qwen2.5-VL-72B-Instruct", ProviderProtocol.OPENAI);

            ChatModel ocrModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, ocrProviderConfig);


            final ChatResponse chat = ocrModel.chat(userMessage);
            final String text = chat.aiMessage().text();

            GraphIngestionRequest extractedGraph = JSON.parseObject(JSON.toJSONString(text),
                    GraphIngestionRequest.class);

            if (extractedGraph != null && extractedGraph.getEntities() != null && !extractedGraph.getEntities().isEmpty()) {
                // 保存提取的图数据到Neo4j
                graphIngestionService.ingestGraphData(extractedGraph);
                log.info("成功提取并保存文档 {} 的知识图谱，实体数: {}, 关系数: {}",
                        extractedGraph.getDocumentId(), 
                        extractedGraph.getEntities().size(),
                        extractedGraph.getRelationships() != null ? extractedGraph.getRelationships().size() : 0);
            } else {
            }

        } catch (Exception e) {
            // 在实际生产环境中，这里应该实现重试机制或将消息发送到死信队列
        }
    }
    /** 从消息中创建OCR模型
     *
     * @param ragDocSyncOcrMessage OCR消息
     * @return ChatModel实例
     * @throws RuntimeException 如果没有配置OCR模型或创建失败 */
    private ChatModel createOcrModelFromMessage(RagDocSyncOcrMessage ragDocSyncOcrMessage) {
        // 检查消息和模型配置是否存在
        if (ragDocSyncOcrMessage == null || ragDocSyncOcrMessage.getOcrModelConfig() == null) {
            String errorMsg = String.format("用户 %s 未配置OCR模型，无法进行文档OCR处理",
                    ragDocSyncOcrMessage != null ? ragDocSyncOcrMessage.getUserId() : "unknown");
            log.error(errorMsg);
            throw new BusinessException(errorMsg);
        }

        try {
            var modelConfig = ragDocSyncOcrMessage.getOcrModelConfig();

            // 验证模型配置的完整性
            if (modelConfig.getModelId() == null || modelConfig.getApiKey() == null
                    || modelConfig.getBaseUrl() == null) {
                String errorMsg = String.format("用户 %s 的OCR模型配置不完整: modelId=%s, apiKey=%s, baseUrl=%s",
                        ragDocSyncOcrMessage.getUserId(), modelConfig.getModelId(),
                        modelConfig.getApiKey() != null ? "已配置" : "未配置", modelConfig.getBaseUrl());
                log.error(errorMsg);
                throw new BusinessException(errorMsg);
            }

            ProviderConfig ocrProviderConfig = new ProviderConfig(modelConfig.getApiKey(), modelConfig.getBaseUrl(),
                    modelConfig.getModelId(), ProviderProtocol.OPENAI);

            ChatModel ocrModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, ocrProviderConfig);

            log.info("Successfully created OCR model for user {}: {}", ragDocSyncOcrMessage.getUserId(),
                    modelConfig.getModelId());
            return ocrModel;

        } catch (RuntimeException e) {
            // 重新抛出已知的业务异常
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("用户 %s 创建OCR模型失败: %s", ragDocSyncOcrMessage.getUserId(), e.getMessage());
            log.error(errorMsg, e);
            throw new BusinessException(errorMsg, e);
        }
    }
}
