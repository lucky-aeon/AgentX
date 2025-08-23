package org.xhy.application.conversation.service.message.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.xhy.application.rag.service.manager.RagQaDatasetAppService;
import org.xhy.application.rag.dto.DocumentUnitDTO;
import org.xhy.application.rag.dto.RagSearchRequest;
import org.xhy.application.rag.service.search.RAGSearchAppService;

import java.util.ArrayList;
import java.util.List;

/** RAG工具执行器 用于执行具体的RAG搜索逻辑，支持通过构造函数传入多个知识库ID避免并发问题 不能交给Spring管理，需要为每个会话创建独立实例 */
public class RagToolExecutor implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(RagToolExecutor.class);

    private final List<String> knowledgeBaseIds;
    private final String userId;
    private final RAGSearchAppService ragSearchAppService;
    private final ObjectMapper objectMapper;

    /** 构造函数
     * @param knowledgeBaseIds RAG知识库ID列表
     * @param userId 用户ID
     * @param ragSearchAppService RAG服务 */
    public RagToolExecutor(List<String> knowledgeBaseIds, String userId,
                           RAGSearchAppService ragSearchAppService) {
        this.knowledgeBaseIds = knowledgeBaseIds != null ? new ArrayList<>(knowledgeBaseIds) : new ArrayList<>();
        this.userId = userId;
        this.ragSearchAppService = ragSearchAppService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        try {
            log.info("执行RAG工具搜索，knowledgeBaseIds: {}, userId: {}, memoryId: {}", knowledgeBaseIds, userId, memoryId);

            // 检查是否有配置的知识库
            if (knowledgeBaseIds.isEmpty()) {
                return "错误：未配置任何知识库";
            }

            // 解析工具执行请求参数
            String arguments = toolExecutionRequest.arguments();
            if (!StringUtils.hasText(arguments)) {
                return "错误：搜索参数为空";
            }

            // 解析JSON参数
            JsonNode argsNode = objectMapper.readTree(arguments);
            String query = argsNode.has("query") ? argsNode.get("query").asText() : "";
            int maxResults = argsNode.has("maxResults") ? argsNode.get("maxResults").asInt(10) : 10;
            double minScore = argsNode.has("minScore") ? argsNode.get("minScore").asDouble(0.5) : 0.5;
            boolean enableRerank = argsNode.has("enableRerank") ? argsNode.get("enableRerank").asBoolean(true) : true;
            boolean enableQueryExpansion = argsNode.has("enableQueryExpansion")
                    ? argsNode.get("enableQueryExpansion").asBoolean(false)
                    : false;

            if (!StringUtils.hasText(query)) {
                return "错误：搜索查询内容为空";
            }

            log.debug(
                    "RAG搜索参数 - query: {}, maxResults: {}, minScore: {}, enableRerank: {}, enableQueryExpansion: {}, knowledgeBaseCount: {}",
                    query, maxResults, minScore, enableRerank, enableQueryExpansion, knowledgeBaseIds.size());

            // 构建RAG搜索请求，支持多个知识库
            RagSearchRequest searchRequest = new RagSearchRequest();
            searchRequest.setDatasetIds(knowledgeBaseIds);
            searchRequest.setQuestion(query);
            searchRequest.setMaxResults(maxResults);
            searchRequest.setMinScore(minScore);
            searchRequest.setEnableRerank(enableRerank);
            searchRequest.setEnableQueryExpansion(enableQueryExpansion);

            // 执行RAG搜索
            List<DocumentUnitDTO> searchResults = ragSearchAppService.ragSearch(searchRequest, userId);

            if (searchResults == null || searchResults.isEmpty()) {
                log.info("RAG搜索未找到相关文档，knowledgeBaseIds: {}, query: {}", knowledgeBaseIds, query);
                return "未找到相关文档内容";
            }

            // 格式化搜索结果
            String formattedResults = formatSearchResults(searchResults, query);

            log.info("RAG搜索完成，knowledgeBaseIds: {}, query: {}, 找到文档数量: {}", knowledgeBaseIds, query,
                    searchResults.size());

            return formattedResults;

        } catch (Exception e) {
            log.error("RAG工具执行失败，knowledgeBaseIds: {}, userId: {}, error: {}", knowledgeBaseIds, userId, e.getMessage(),
                    e);
            return "搜索过程中发生错误：" + e.getMessage();
        }
    }

    /** 格式化搜索结果
     * @param searchResults 搜索结果列表
     * @param query 搜索查询
     * @return 格式化后的结果字符串 */
    private String formatSearchResults(List<DocumentUnitDTO> searchResults, String query) {
        StringBuilder result = new StringBuilder();
        result.append("根据查询「").append(query).append("」找到以下相关内容：\n\n");

        for (int i = 0; i < searchResults.size(); i++) {
            DocumentUnitDTO doc = searchResults.get(i);
            result.append("【文档片段 ").append(i + 1).append("】\n");

            // 添加文档内容
            if (StringUtils.hasText(doc.getContent())) {
                String content = doc.getContent().trim();
                // 限制单个文档片段的长度，避免响应过长
                if (content.length() > 500) {
                    content = content.substring(0, 500) + "...";
                }
                result.append(content).append("\n");
            }

            // 添加来源信息（如果有文件ID）
            if (StringUtils.hasText(doc.getFileId())) {
                result.append("来源：文件ID ").append(doc.getFileId());
                if (doc.getPage() != null) {
                    result.append("，第 ").append(doc.getPage()).append(" 页");
                }
                result.append("\n");
            }

            result.append("\n");
        }

        result.append("以上内容来自知识库，请基于这些信息回答用户问题。");

        return result.toString();
    }

    /** 获取知识库ID列表
     * @return 知识库ID列表 */
    public List<String> getKnowledgeBaseIds() {
        return new ArrayList<>(knowledgeBaseIds);
    }

    /** 获取用户ID
     * @return 用户ID */
    public String getUserId() {
        return userId;
    }
}
