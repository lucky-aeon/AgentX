package org.xhy.application.knowledgeGraph.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.domain.knowledgeGraph.message.DocIeInferMessage;
import org.xhy.domain.neo4j.constant.EnhancedGraphExtractorPrompt;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.service.DocumentUnitDomainService;
import org.xhy.infrastructure.json.Neo4jCompatibleJsonParser;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 上下文感知的图谱提取服务
 * 解决分页处理中的上下文丢失问题
 * 
 * @author AgentX
 */
@Service
public class ContextAwareGraphExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(ContextAwareGraphExtractionService.class);

    private final DocumentUnitDomainService documentUnitDomainService;

    public ContextAwareGraphExtractionService(DocumentUnitDomainService documentUnitDomainService) {
        this.documentUnitDomainService = documentUnitDomainService;
    }

    /**
     * 上下文感知的图谱提取
     * 
     * @param message 分页消息
     * @return 提取的图谱数据
     */
    public GraphIngestionRequest extractWithContext(DocIeInferMessage message) {
        String fileId = message.getFileId();
        Integer pageNumber = message.getPageNumber();
        Integer totalPages = message.getTotalPages();
        String documentText = message.getDocumentText();

        logger.info("开始上下文感知图谱提取，文档: {}, 页码: {}/{}", fileId, pageNumber, totalPages);

        try {
            // 获取上下文信息
            String previousContext = getPreviousContext(fileId, pageNumber);
            String nextContext = getNextContext(fileId, pageNumber, totalPages);

            // 构建增强的提示词
            String enhancedPrompt = buildContextAwarePrompt(
                    documentText, pageNumber, totalPages, previousContext, nextContext);

            // 调用AI进行提取
            GraphIngestionRequest extractedGraph = callAIForExtraction(enhancedPrompt);

            // 设置基本信息
            if (extractedGraph != null) {
                extractedGraph.setDocumentId(fileId);
                
                // 为所有实体和关系设置页面信息
                enrichWithPageInfo(extractedGraph, fileId, pageNumber);
            }

            logger.info("上下文感知图谱提取完成，实体数: {}, 关系数: {}",
                    extractedGraph != null && extractedGraph.getEntities() != null ? extractedGraph.getEntities().size() : 0,
                    extractedGraph != null && extractedGraph.getRelationships() != null ? extractedGraph.getRelationships().size() : 0);

            return extractedGraph;

        } catch (Exception e) {
            logger.error("上下文感知图谱提取失败，文档: {}, 页码: {}", fileId, pageNumber, e);
            throw new RuntimeException("图谱提取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取前一页的上下文信息
     */
    private String getPreviousContext(String fileId, Integer pageNumber) {
        if (pageNumber == null || pageNumber <= 1) {
            return null;
        }

        try {
            List<DocumentUnitEntity> allDocuments = documentUnitDomainService.listDocumentUnitsByFileId(fileId);
            
            // 找到前一页的内容
            DocumentUnitEntity previousPage = allDocuments.stream()
                    .filter(doc -> doc.getPage() != null && doc.getPage().equals(pageNumber - 1))
                    .findFirst()
                    .orElse(null);

            if (previousPage != null && previousPage.getContent() != null) {
                // 提取关键实体和概念（简化版本，可以使用AI进一步提取关键信息）
                String content = previousPage.getContent();
                return extractKeyContext(content, "previous");
            }

        } catch (Exception e) {
            logger.warn("获取前一页上下文失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 获取下一页的预览信息
     */
    private String getNextContext(String fileId, Integer pageNumber, Integer totalPages) {
        if (pageNumber == null || totalPages == null || pageNumber >= totalPages) {
            return null;
        }

        try {
            List<DocumentUnitEntity> allDocuments = documentUnitDomainService.listDocumentUnitsByFileId(fileId);
            
            // 找到下一页的内容
            DocumentUnitEntity nextPage = allDocuments.stream()
                    .filter(doc -> doc.getPage() != null && doc.getPage().equals(pageNumber + 1))
                    .findFirst()
                    .orElse(null);

            if (nextPage != null && nextPage.getContent() != null) {
                String content = nextPage.getContent();
                // 只取开头部分作为预览
                String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                return extractKeyContext(preview, "next");
            }

        } catch (Exception e) {
            logger.warn("获取下一页预览失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 提取关键上下文信息（简化实现）
     * 实际项目中可以使用更复杂的NLP或AI方法
     */
    private String extractKeyContext(String content, String type) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // 简化实现：提取关键词和短语
        // 可以替换为更复杂的实体提取算法
        String[] sentences = content.split("[。！？.!?]");
        StringBuilder keyContext = new StringBuilder();
        
        for (int i = 0; i < Math.min(3, sentences.length); i++) {
            String sentence = sentences[i].trim();
            if (sentence.length() > 10) { // 过滤太短的句子
                keyContext.append(sentence).append("。");
            }
        }

        return keyContext.toString();
    }

    /**
     * 构建上下文感知的提示词
     */
    private String buildContextAwarePrompt(String documentText, Integer pageNumber, Integer totalPages,
                                           String previousContext, String nextContext) {
        
        String prompt = EnhancedGraphExtractorPrompt.contextAwareExtractionPrompt;
        
        // 替换模板变量
        prompt = prompt.replace("{{pageNumber}}", String.valueOf(pageNumber));
        prompt = prompt.replace("{{totalPages}}", String.valueOf(totalPages));
        prompt = prompt.replace("{{text}}", documentText);

        // 处理上下文信息
        if (previousContext != null && !previousContext.isEmpty()) {
            prompt = prompt.replace("{{#hasPreviousContext}}", "");
            prompt = prompt.replace("{{/hasPreviousContext}}", "");
            prompt = prompt.replace("{{previousContext}}", previousContext);
        } else {
            // 移除上下文相关的部分
            prompt = prompt.replaceAll("\\{\\{#hasPreviousContext\\}\\}[\\s\\S]*?\\{\\{/hasPreviousContext\\}\\}", "");
        }

        if (nextContext != null && !nextContext.isEmpty()) {
            prompt = prompt.replace("{{#hasNextContext}}", "");
            prompt = prompt.replace("{{/hasNextContext}}", "");
            prompt = prompt.replace("{{nextContext}}", nextContext);
        } else {
            // 移除预览相关的部分
            prompt = prompt.replaceAll("\\{\\{#hasNextContext\\}\\}[\\s\\S]*?\\{\\{/hasNextContext\\}\\}", "");
        }

        return prompt;
    }

    /**
     * 调用AI进行图谱提取
     */
    private GraphIngestionRequest callAIForExtraction(String prompt) {
        try {
            // 创建LLM配置（从消息中获取用户配置的模型）
            ProviderConfig providerConfig = new ProviderConfig(
                    "sk-b9aL4HqXa1OHY6TyctHBVnjq9IQndYd5snq4WdX9sQ4DUFma", 
                    "https://new.281182.xyz/v1",
                    "gemini-2.5-pro", 
                    ProviderProtocol.OPENAI);

            ChatModel chatModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, providerConfig);

            final UserMessage userMessage = UserMessage.userMessage(TextContent.from(prompt));
            final ChatResponse chat = chatModel.chat(userMessage);
            final String responseText = chat.aiMessage().text();

            logger.debug("AI返回的原始文本: {}", responseText);

            // 清理和解析JSON
            String cleanedJson = extractJsonFromText(responseText);
            logger.debug("清理后的JSON: {}", cleanedJson);

            // 解析为GraphIngestionRequest，避免BigDecimal问题
            return Neo4jCompatibleJsonParser.parseObject(cleanedJson, GraphIngestionRequest.class);

        } catch (Exception e) {
            logger.error("AI图谱提取调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从AI返回的文本中提取JSON内容
     */
    private String extractJsonFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "{}";
        }

        text = text.trim();

        // 处理Markdown代码块
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                int lastTripleBacktick = text.lastIndexOf("```");
                if (lastTripleBacktick > firstNewline) {
                    text = text.substring(firstNewline + 1, lastTripleBacktick).trim();
                }
            }
        }

        // 提取JSON部分
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1);
        }

        // 清理格式问题
        text = cleanJsonString(text);

        if (!text.startsWith("{") || !text.endsWith("}")) {
            logger.warn("无法从文本中提取有效的JSON格式，返回空对象");
            return "{}";
        }

        return text;
    }

    /**
     * 清理JSON字符串
     */
    private String cleanJsonString(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return jsonStr;
        }

        // 移除BOM字符
        if (jsonStr.startsWith("\uFEFF")) {
            jsonStr = jsonStr.substring(1);
        }

        // 处理转义问题
        jsonStr = jsonStr.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r");

        return jsonStr.trim();
    }

    /**
     * 为提取的图谱数据添加页面信息
     */
    private void enrichWithPageInfo(GraphIngestionRequest request, String fileId, Integer pageNumber) {
        // 为实体添加页面信息
        if (request.getEntities() != null) {
            request.getEntities().forEach(entity -> {
                entity.setFileId(fileId);
                entity.setPageNumber(pageNumber);
                
                // 确保实体属性中包含页面信息
                if (entity.getProperties() != null) {
                    entity.getProperties().put("pageNumber", pageNumber);
                    entity.getProperties().put("fileId", fileId);
                }
            });
        }

        // 为关系添加页面信息
        if (request.getRelationships() != null) {
            request.getRelationships().forEach(relationship -> {
                relationship.setFileId(fileId);
                relationship.setPageNumber(pageNumber);
                
                // 确保关系属性中包含页面信息
                if (relationship.getProperties() != null) {
                    relationship.getProperties().put("pageNumber", pageNumber);
                    relationship.getProperties().put("fileId", fileId);
                }
            });
        }
    }
}
