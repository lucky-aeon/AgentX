package org.xhy.domain.knowledgeGraph.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionResponse;
import org.xhy.application.knowledgeGraph.dto.GraphQueryRequest;
import org.xhy.application.knowledgeGraph.dto.GraphQueryResponse;
import org.xhy.domain.knowledgeGraph.repository.KnowledgeGraphRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.Map;

/**
 * 知识图谱领域服务
 * 负责知识图谱相关的核心业务逻辑和领域规则
 * 
 * @author shilong.zang
 * @since 1.0.0
 */
@Service
public class KnowledgeGraphDomainService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphDomainService.class);

    private final KnowledgeGraphRepository knowledgeGraphRepository;

    /**
     * 构造函数
     * 
     * @param knowledgeGraphRepository 知识图谱仓储
     */
    public KnowledgeGraphDomainService(KnowledgeGraphRepository knowledgeGraphRepository) {
        this.knowledgeGraphRepository = knowledgeGraphRepository;
    }

    /**
     * 摄取知识图谱数据
     * 执行图数据摄取的领域逻辑，包括数据验证和业务规则检查
     * 
     * @param request 图数据摄取请求
     * @return 摄取结果响应
     * @throws BusinessException 当业务规则验证失败时抛出
     */
    public GraphIngestionResponse ingestGraphData(GraphIngestionRequest request) {
        // 领域规则验证
        validateIngestionRequest(request);

        // 委托给仓储层执行数据持久化
        return knowledgeGraphRepository.ingestGraphData(request);
    }

    /**
     * 执行知识图谱查询
     * 执行图查询的领域逻辑，包括查询参数验证和结果处理
     * 
     * @param request 图查询请求
     * @return 查询结果响应
     * @throws BusinessException 当查询参数无效时抛出
     */
    public GraphQueryResponse executeQuery(GraphQueryRequest request) {
        // 领域规则验证
        validateQueryRequest(request);

        // 委托给仓储层执行查询
        return knowledgeGraphRepository.executeQuery(request);
    }

    /**
     * 查找两个节点之间的路径
     * 
     * @param sourceNodeId 源节点ID
     * @param targetNodeId 目标节点ID
     * @param maxDepth 最大搜索深度
     * @return 路径查询结果
     * @throws BusinessException 当节点不存在时抛出
     */
    public GraphQueryResponse findPathBetweenNodes(String sourceNodeId, String targetNodeId, Integer maxDepth) {
        // 验证节点存在性
        if (!knowledgeGraphRepository.existsNode(sourceNodeId)) {
            throw new BusinessException("源节点不存在: " + sourceNodeId);
        }
        if (!knowledgeGraphRepository.existsNode(targetNodeId)) {
            throw new BusinessException("目标节点不存在: " + targetNodeId);
        }

        return knowledgeGraphRepository.findPathBetweenNodes(sourceNodeId, targetNodeId, maxDepth);
    }

    /**
     * 获取知识图谱统计信息
     * 
     * @return 统计信息Map
     */
    public Map<String, Object> getGraphStatistics() {
        return knowledgeGraphRepository.getGraphStatistics();
    }

    /**
     * 检查节点是否存在
     * 
     * @param nodeId 节点ID
     * @return true如果存在，false如果不存在
     */
    public boolean existsNode(String nodeId) {
        if (nodeId == null || nodeId.trim().isEmpty()) {
            return false;
        }
        return knowledgeGraphRepository.existsNode(nodeId);
    }

    /**
     * 删除指定文档的图数据
     * 
     * @param documentId 文档ID
     * @return 删除统计信息
     */
    public Map<String, Integer> deleteGraphDataByDocument(String documentId) {
        if (documentId == null || documentId.trim().isEmpty()) {
            throw new BusinessException("文档ID不能为空");
        }

        logger.info("Start deleting graph data for document: {}", documentId);
        Map<String, Integer> result = knowledgeGraphRepository.deleteGraphDataByDocument(documentId);
        logger.info("Graph data deletion completed for document: {}, nodes: {}, relationships: {}", 
                documentId, result.get("nodes"), result.get("relationships"));

        return result;
    }

    /**
     * 验证图数据摄取请求
     * 
     * @param request 摄取请求
     * @throws BusinessException 当验证失败时抛出
     */
    private void validateIngestionRequest(GraphIngestionRequest request) {
        if (request == null) {
            throw new BusinessException("摄取请求不能为空");
        }

        if (request.getDocumentId() == null || request.getDocumentId().trim().isEmpty()) {
            throw new BusinessException("文档ID不能为空");
        }

        // 验证实体数据
        if (request.getEntities() != null) {
            for (GraphIngestionRequest.EntityDto entity : request.getEntities()) {
                if (entity.getId() == null || entity.getId().trim().isEmpty()) {
                    throw new BusinessException("实体ID不能为空");
                }
            }
        }

        // 验证关系数据
        if (request.getRelationships() != null) {
            for (GraphIngestionRequest.RelationshipDto relationship : request.getRelationships()) {
                if (relationship.getSourceId() == null || relationship.getSourceId().trim().isEmpty()) {
                    throw new BusinessException("关系源节点ID不能为空");
                }
                if (relationship.getTargetId() == null || relationship.getTargetId().trim().isEmpty()) {
                    throw new BusinessException("关系目标节点ID不能为空");
                }
                if (relationship.getType() == null || relationship.getType().trim().isEmpty()) {
                    throw new BusinessException("关系类型不能为空");
                }
            }
        }
    }

    /**
     * 验证图查询请求
     * 
     * @param request 查询请求
     * @throws BusinessException 当验证失败时抛出
     */
    private void validateQueryRequest(GraphQueryRequest request) {
        if (request == null) {
            throw new BusinessException("查询请求不能为空");
        }

        if (request.getStartNodes() == null || request.getStartNodes().isEmpty()) {
            throw new BusinessException("起始节点不能为空");
        }

        // 验证起始节点过滤条件
        for (GraphQueryRequest.NodeFilter nodeFilter : request.getStartNodes()) {
            if (nodeFilter.getProperty() != null && nodeFilter.getValue() == null) {
                throw new BusinessException("节点属性过滤值不能为空");
            }
        }
    }
}
