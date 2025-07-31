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
import org.xhy.domain.rag.service.UserRagDomainService;

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
    private final UserRagDomainService userRagDomainService;

    public RagToolManager(RagQaDatasetAppService ragQaDatasetAppService,
            RagQaDatasetDomainService ragQaDatasetDomainService, UserRagDomainService userRagDomainService) {
        this.ragQaDatasetAppService = ragQaDatasetAppService;
        this.ragQaDatasetDomainService = ragQaDatasetDomainService;
        this.userRagDomainService = userRagDomainService;
    }

    /** 为Agent创建RAG工具（如果Agent配置了知识库）
     * @param agent Agent实体
     * @return RAG工具映射，如果没有配置知识库则返回空Map */
    public Map<ToolSpecification, ToolExecutor> createRagTools(AgentEntity agent) {
        List<String> knowledgeBaseIds = agent.getKnowledgeBaseIds();

        // 如果没有配置知识库，返回空Map
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return null;
        }

        try {
            // 验证知识库是否存在且用户有权限访问
            List<String> validKnowledgeBaseIds = validateKnowledgeBases(knowledgeBaseIds, agent.getUserId());

            if (validKnowledgeBaseIds.isEmpty()) {
                log.warn("Agent {} 配置的知识库都无效或无权限访问", agent.getId());
                return null;
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
            return null;
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
                // 检查用户是否安装了这个知识库
                boolean isInstalled = userRagDomainService.isRagInstalledByOriginalId(userId, knowledgeBaseId);

                if (isInstalled) {
                    validIds.add(knowledgeBaseId);
                    log.debug("知识库 {} 验证通过，用户已安装", knowledgeBaseId);
                } else {
                    log.warn("知识库 {} 验证失败，用户 {} 未安装该知识库", knowledgeBaseId, userId);
                }
            } catch (Exception e) {
                log.warn("知识库 {} 验证失败: {}", knowledgeBaseId, e.getMessage());
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
                // 获取用户安装的知识库信息
                var userRag = userRagDomainService.findInstalledRagByOriginalId(userId, id);
                if (userRag != null) {
                    // 用户已安装，直接使用安装记录中的名称
                    // 无论是SNAPSHOT还是REFERENCE类型，都使用安装记录中的信息
                    return userRag.getName();
                } else {
                    // 用户未安装该知识库，不应该能访问
                    log.warn("用户 {} 未安装知识库 {}，无法获取名称", userId, id);
                    return "未知知识库";
                }
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
