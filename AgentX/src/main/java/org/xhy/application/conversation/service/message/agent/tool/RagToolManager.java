package org.xhy.application.conversation.service.message.agent.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.application.rag.service.RagQaDatasetAppService;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.service.RagQaDatasetDomainService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** RAG工具管理器 负责创建和管理Agent的RAG工具，支持多知识库集成 */
@Component
public class RagToolManager {

    private static final Logger log = LoggerFactory.getLogger(RagToolManager.class);

    private final RagQaDatasetAppService ragQaDatasetAppService;
    private final RagQaDatasetDomainService ragQaDatasetDomainService;

    public RagToolManager(RagQaDatasetAppService ragQaDatasetAppService,
            RagQaDatasetDomainService ragQaDatasetDomainService) {
        this.ragQaDatasetAppService = ragQaDatasetAppService;
        this.ragQaDatasetDomainService = ragQaDatasetDomainService;
    }

    /** 为Agent创建RAG工具（如果Agent配置了知识库）
     * @param agent Agent实体
     * @return RAG工具映射，如果没有配置知识库则返回空Map */
    public Map<ToolSpecification, ToolExecutor> createRagTools(AgentEntity agent) {
        List<String> knowledgeBaseIds = agent.getKnowledgeBaseIds();

        // 如果没有配置知识库，返回空Map
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            // 验证知识库是否存在且用户有权限访问
            List<String> validKnowledgeBaseIds = validateKnowledgeBases(knowledgeBaseIds, agent.getUserId());

            if (validKnowledgeBaseIds.isEmpty()) {
                log.warn("Agent {} 配置的知识库都无效或无权限访问", agent.getId());
                return new HashMap<>();
            }

            // 获取知识库名称用于工具描述
            List<String> knowledgeBaseNames = getKnowledgeBaseNames(validKnowledgeBaseIds, agent.getUserId());

            // 创建RAG工具规范
            ToolSpecification ragToolSpec = RagToolSpecification.createToolSpecification(knowledgeBaseNames);

            // 创建RAG工具执行器
            RagToolExecutor ragToolExecutor = new RagToolExecutor(validKnowledgeBaseIds, agent.getUserId(),
                    ragQaDatasetAppService);

            Map<ToolSpecification, ToolExecutor> ragTools = new HashMap<>();
            ragTools.put(ragToolSpec, ragToolExecutor);

            log.info("为Agent {} 创建RAG工具成功，关联知识库数量: {}", agent.getId(), validKnowledgeBaseIds.size());

            return ragTools;

        } catch (Exception e) {
            log.error("为Agent {} 创建RAG工具失败: {}", agent.getId(), e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /** 验证知识库是否存在且用户有权限访问
     * @param knowledgeBaseIds 知识库ID列表
     * @param userId 用户ID
     * @return 有效的知识库ID列表 */
    private List<String> validateKnowledgeBases(List<String> knowledgeBaseIds, String userId) {
        List<String> validIds = new ArrayList<>();

        for (String knowledgeBaseId : knowledgeBaseIds) {
            try {
                // 检查知识库是否存在且用户有权限访问
                ragQaDatasetDomainService.checkDatasetExists(knowledgeBaseId, userId);
                validIds.add(knowledgeBaseId);
                log.debug("知识库 {} 验证通过", knowledgeBaseId);
            } catch (Exception e) {
                log.warn("知识库 {} 验证失败，用户 {} 无权限访问: {}", knowledgeBaseId, userId, e.getMessage());
            }
        }

        return validIds;
    }

    /** 获取知识库名称列表
     * @param knowledgeBaseIds 知识库ID列表
     * @param userId 用户ID
     * @return 知识库名称列表 */
    private List<String> getKnowledgeBaseNames(List<String> knowledgeBaseIds, String userId) {
        return knowledgeBaseIds.stream().map(id -> {
            try {
                RagQaDatasetEntity dataset = ragQaDatasetDomainService.getDataset(id, userId);
                return dataset.getName();
            } catch (Exception e) {
                log.warn("获取知识库 {} 名称失败: {}", id, e.getMessage());
                return "未知知识库";
            }
        }).collect(Collectors.toList());
    }

    /** 检查Agent是否配置了RAG工具
     * @param agent Agent实体
     * @return 是否配置了RAG工具 */
    public boolean hasRagTools(AgentEntity agent) {
        List<String> knowledgeBaseIds = agent.getKnowledgeBaseIds();
        return knowledgeBaseIds != null && !knowledgeBaseIds.isEmpty();
    }

    /** 获取Agent配置的知识库数量
     * @param agent Agent实体
     * @return 知识库数量 */
    public int getKnowledgeBaseCount(AgentEntity agent) {
        List<String> knowledgeBaseIds = agent.getKnowledgeBaseIds();
        return knowledgeBaseIds != null ? knowledgeBaseIds.size() : 0;
    }
}
