package org.xhy.domain.knowledgeGraph;

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
import org.xhy.application.knowledgeGraph.service.GraphIngestionService;
import org.xhy.domain.knowledgeGraph.message.DocIeInferMessage;
import org.xhy.domain.neo4j.constant.GraphExtractorPrompt;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;
import org.xhy.infrastructure.mq.events.DocIeInferEvent;
import org.xhy.infrastructure.mq.model.MqMessage;

/**
 * 文档信息抽取推理消费者
 * 使用LangChain4j从文档文本中提取知识图谱数据
 * 
 * @author shilong.zang
 * @date 15:13 <br/>
 */
@Component
public class DocIeInferConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocIeInferConsumer.class);

    // AI服务和图数据存储服务
    private final GraphIngestionService graphIngestionService;

    public DocIeInferConsumer( GraphIngestionService graphIngestionService) {
        this.graphIngestionService = graphIngestionService;
    }
    
    /**
     * 从AI返回的文本中提取JSON内容
     * 处理可能包含Markdown代码块或其他格式的文本
     */
    private String extractJsonFromText(String text) {
        if (StrUtil.isBlank(text)) {
            return "{}";
        }
        
        // 去除首尾空白
        text = text.trim();
        
        // 如果文本以```json或```开头，提取代码块中的内容
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                int lastTripleBacktick = text.lastIndexOf("```");
                if (lastTripleBacktick > firstNewline) {
                    text = text.substring(firstNewline + 1, lastTripleBacktick).trim();
                }
            }
        }
        
        // 查找第一个{和最后一个}，提取JSON部分
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1);
        }
        
        // 清理可能的转义字符和格式问题
        text = cleanJsonString(text);
        
        // 如果还是不是有效的JSON格式，返回空对象
        if (!text.startsWith("{") || !text.endsWith("}")) {
            log.warn("无法从文本中提取有效的JSON格式，返回空对象");
            return "{}";
        }
        
        return text;
    }
    
    /**
     * 清理JSON字符串中的格式问题
     */
    private String cleanJsonString(String jsonStr) {
        if (StrUtil.isBlank(jsonStr)) {
            return jsonStr;
        }
        
        // 移除可能的BOM字符
        if (jsonStr.startsWith("\uFEFF")) {
            jsonStr = jsonStr.substring(1);
        }
        
        // 处理可能的转义问题
        jsonStr = jsonStr.replace("\\n", "\n")
                        .replace("\\t", "\t")
                        .replace("\\r", "\r");
        
        // 移除多余的空白字符，但保留JSON结构
        jsonStr = jsonStr.trim();
        
        return jsonStr;
    }

    /**
     * 处理文档信息抽取推理事件
     * 
     */
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(DocIeInferEvent.QUEUE_NAME), 
        exchange = @Exchange(value = DocIeInferEvent.EXCHANGE_NAME, type = ExchangeTypes.TOPIC), 
        key = DocIeInferEvent.ROUTE_KEY))
    public void receiveMessage(Message message, String msg, Channel channel) throws IOException {
        
        log.info("DocIeInferConsumer 收到消息: {}", msg);

        MqMessage mqMessageBody = JSONObject.parseObject(msg, MqMessage.class);

        MDC.put(HEADER_NAME_TRACE_ID,
                Objects.nonNull(mqMessageBody.getTraceId()) ? mqMessageBody.getTraceId() : IdWorker.getTimeId());
        MessageProperties messageProperties = message.getMessageProperties();
        long deliveryTag = messageProperties.getDeliveryTag();
        DocIeInferMessage ocrMessage = JSON.parseObject(JSON.toJSONString(mqMessageBody.getData()),
                DocIeInferMessage.class);
        
        log.info("开始处理文档 {} 的知识图谱提取任务", ocrMessage.getFileId());

        boolean messageProcessed = false; // 标记消息是否已经被处理（确认或拒绝）

        try {
            // 使用AI服务从文本中提取知识图谱
            String documentText = ocrMessage.getDocumentText();
            log.info("文档内容长度: {}", documentText != null ? documentText.length() : "null");
            
            // 替换prompt模板中的占位符
            final String formattedPrompt = GraphExtractorPrompt.graphExtractorPrompt.replace("{{text}}", documentText);
            final UserMessage userMessage = UserMessage.userMessage(
                    TextContent.from(formattedPrompt));
            
            log.info("准备发送给AI的prompt长度: {}", formattedPrompt.length());

            /** 创建OCR处理的模型配置 - 从消息中获取用户配置的OCR模型 */
            ProviderConfig ocrProviderConfig = new ProviderConfig(
                    "sk-cxdmubeuwhayavsalqgmkrljfplhharyrociewxaikfmqkwm", "https://api.siliconflow.cn/v1",
                    "Qwen/Qwen2.5-VL-72B-Instruct", ProviderProtocol.OPENAI);

            ChatModel ocrModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, ocrProviderConfig);

            final ChatResponse chat = ocrModel.chat(userMessage);
            final String text = chat.aiMessage().text();
            
            log.info("AI返回的原始文本: {}", text);
            
            // 清理和提取JSON内容
            String cleanedJson = extractJsonFromText(text);
            log.info("清理后的JSON: {}", cleanedJson);

            GraphIngestionRequest extractedGraph = null;
            try {
                extractedGraph = JSON.parseObject(cleanedJson, GraphIngestionRequest.class);
            } catch (Exception jsonException) {
                log.error("JSON解析失败，原始文本: {}", text);
                log.error("清理后的JSON: {}", cleanedJson);
                log.error("JSON解析异常: {}", jsonException.getMessage(), jsonException);
                
                // 尝试使用fastjson2的其他解析方式
                try {
                    extractedGraph = JSONObject.parseObject(cleanedJson, GraphIngestionRequest.class);
                    log.info("使用JSONObject.parseObject解析成功");
                } catch (Exception secondException) {
                    log.error("第二次JSON解析也失败: {}", secondException.getMessage(), secondException);
                    // 返回空的GraphIngestionRequest，让后续逻辑处理
                    extractedGraph = null;
                }
            }

            if (extractedGraph != null && extractedGraph.getEntities() != null && !extractedGraph.getEntities().isEmpty()) {
                // 设置文档ID
                extractedGraph.setDocumentId(ocrMessage.getFileId());
                // 保存提取的图数据到Neo4j
                graphIngestionService.ingestGraphData(extractedGraph);
                log.info("成功提取并保存文档 {} 的知识图谱，实体数: {}, 关系数: {}",
                        extractedGraph.getDocumentId(), 
                        extractedGraph.getEntities().size(),
                        extractedGraph.getRelationships() != null ? extractedGraph.getRelationships().size() : 0);
            } else {
                log.warn("未能从文档 {} 中提取到有效的知识图谱数据", ocrMessage.getFileId());
            }
            
            // 成功处理完成，确认消息
            channel.basicAck(deliveryTag, false);
            messageProcessed = true;
            log.info("文档 {} 处理完成，消息已确认", ocrMessage.getFileId());

        } catch (Exception e) {
            log.error("处理文档 {} 的知识图谱提取任务失败: {}", ocrMessage.getFileId(), e.getMessage(), e);
            try {
                // 拒绝消息并重新入队，可以实现重试机制
                channel.basicNack(deliveryTag, false, true);
                messageProcessed = true;
                log.info("文档 {} 处理失败，消息已拒绝并重新入队", ocrMessage.getFileId());
            } catch (IOException ioException) {
                log.error("消息拒绝失败: {}", ioException.getMessage(), ioException);
                // 如果拒绝失败，尝试确认消息以避免重复消费
                try {
                    channel.basicAck(deliveryTag, false);
                    messageProcessed = true;
                    log.warn("消息拒绝失败，已强制确认消息以避免重复消费");
                } catch (IOException ackException) {
                    log.error("强制确认消息也失败: {}", ackException.getMessage(), ackException);
                }
            }
        } finally {
            // 清理MDC上下文
            MDC.remove(HEADER_NAME_TRACE_ID);
            log.info("DocIeInferConsumer 处理完成，清理MDC上下文，消息处理状态: {}", messageProcessed ? "已处理" : "未处理");
            
            // 如果消息还没有被处理（确认或拒绝），则进行兜底确认
            if (!messageProcessed) {
                try {
                    channel.basicAck(deliveryTag, false);
                    log.warn("执行兜底消息确认，避免消息重复消费");
                } catch (IOException e) {
                    log.error("兜底消息确认失败: {}", e.getMessage(), e);
                }
            }
        }
    }
}
