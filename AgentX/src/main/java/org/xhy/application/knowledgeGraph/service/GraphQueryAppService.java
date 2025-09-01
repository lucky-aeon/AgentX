package org.xhy.application.knowledgeGraph.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.knowledgeGraph.dto.GraphQueryRequest;
import org.xhy.application.knowledgeGraph.dto.GraphQueryResponse;
import org.xhy.domain.knowledgeGraph.service.KnowledgeGraphDomainService;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;
import java.util.Map;

/** 知识图谱查询应用服务 负责协调知识图谱查询的业务流程
 * 
 * @author zang
 * @since 1.0.0 */
@Service
public class GraphQueryAppService {

    private static final Logger logger = LoggerFactory.getLogger(GraphQueryAppService.class);

    private final KnowledgeGraphDomainService knowledgeGraphDomainService;

    /** 构造函数
     * 
     * @param knowledgeGraphDomainService 知识图谱领域服务 */
    public GraphQueryAppService(KnowledgeGraphDomainService knowledgeGraphDomainService) {
        this.knowledgeGraphDomainService = knowledgeGraphDomainService;
    }

    /** 执行知识图谱查询 协调图查询的业务流程，包括参数验证和结果处理
     * 
     * @param request 图查询请求，包含查询条件和返回定义
     * @return 查询结果响应，包含节点和关系数据
     * @throws BusinessException 当查询参数无效或执行失败时抛出 */
    @Transactional(readOnly = true)
    public GraphQueryResponse executeQuery(GraphQueryRequest request) {
        if (request == null) {
            throw new BusinessException("查询请求不能为空");
        }

        long startTime = System.currentTimeMillis();

        try {
            logger.info("Start executing graph query with {} start nodes, {} traversal steps, {} filters",
                    request.getStartNodes() != null ? request.getStartNodes().size() : 0,
                    request.getTraversals() != null ? request.getTraversals().size() : 0,
                    request.getFilters() != null ? request.getFilters().size() : 0);

            // 委托给领域服务执行具体查询逻辑
            GraphQueryResponse response = knowledgeGraphDomainService.executeQuery(request);
            response.setExecutionTime(startTime);

            logger.info("Graph query execution completed, nodes: {}, relationships: {}, execution time: {}ms",
                    response.getNodes() != null ? response.getNodes().size() : 0,
                    response.getRelationships() != null ? response.getRelationships().size() : 0,
                    response.getExecutionTimeMs());

            return response;

        } catch (Exception e) {
            logger.error("Graph query execution failed: {}", e.getMessage(), e);
            throw new BusinessException("查询执行失败: " + e.getMessage(), e);
        }
    }

    /** 根据节点属性查找节点 构建简单的节点查询请求并执行
     * 
     * @param label 节点标签，可以为null
     * @param property 属性名，不能为空
     * @param value 属性值，不能为空
     * @param limit 结果限制，默认100
     * @return 查询结果响应 */
    @Transactional(readOnly = true)
    public GraphQueryResponse findNodesByProperty(String label, String property, Object value, Integer limit) {
        GraphQueryRequest request = new GraphQueryRequest();

        // 设置起始节点
        GraphQueryRequest.NodeFilter nodeFilter = new GraphQueryRequest.NodeFilter(label, property, value);
        request.setStartNodes(List.of(nodeFilter));

        // 设置返回定义
        GraphQueryRequest.ReturnDefinition returnDef = new GraphQueryRequest.ReturnDefinition();
        returnDef.setIncludeNodes(true);
        returnDef.setIncludeRelationships(false);
        request.setReturnDefinition(returnDef);

        // 设置限制
        request.setLimit(limit != null ? limit : 100);

        return executeQuery(request);
    }

    /** 查找节点的关系 构建节点关系查询请求并执行
     * 
     * @param nodeId 节点ID，不能为空
     * @param relationshipType 关系类型，null表示所有类型
     * @param direction 关系方向：OUTGOING, INCOMING, BOTH，默认BOTH
     * @param limit 结果限制，默认100
     * @return 查询结果响应，包含相关节点和关系 */
    @Transactional(readOnly = true)
    public GraphQueryResponse findNodeRelationships(String nodeId, String relationshipType, String direction,
            Integer limit) {
        GraphQueryRequest request = new GraphQueryRequest();

        // 设置起始节点
        GraphQueryRequest.NodeFilter nodeFilter = new GraphQueryRequest.NodeFilter(null, "id", nodeId);
        request.setStartNodes(List.of(nodeFilter));

        // 设置遍历步骤
        GraphQueryRequest.TraversalStep traversal = new GraphQueryRequest.TraversalStep(
                relationshipType != null ? relationshipType : "*", direction != null ? direction : "BOTH");
        request.setTraversals(List.of(traversal));

        // 设置返回定义
        GraphQueryRequest.ReturnDefinition returnDef = new GraphQueryRequest.ReturnDefinition();
        returnDef.setIncludeNodes(true);
        returnDef.setIncludeRelationships(true);
        request.setReturnDefinition(returnDef);

        // 设置限制
        request.setLimit(limit != null ? limit : 100);

        return executeQuery(request);
    }

    /** 查找两个节点之间的路径 委托给领域服务执行路径查询
     * 
     * @param sourceNodeId 源节点ID，不能为空
     * @param targetNodeId 目标节点ID，不能为空
     * @param maxDepth 最大深度，默认5
     * @return 查询结果响应，包含路径上的节点和关系
     * @throws BusinessException 当查询失败时抛出 */
    @Transactional(readOnly = true)
    public GraphQueryResponse findPathBetweenNodes(String sourceNodeId, String targetNodeId, Integer maxDepth) {
        if (sourceNodeId == null || sourceNodeId.trim().isEmpty()) {
            throw new BusinessException("源节点ID不能为空");
        }
        if (targetNodeId == null || targetNodeId.trim().isEmpty()) {
            throw new BusinessException("目标节点ID不能为空");
        }

        return knowledgeGraphDomainService.findPathBetweenNodes(sourceNodeId, targetNodeId,
                maxDepth != null ? maxDepth : 5);
    }

    /** 获取知识图谱统计信息 委托给领域服务获取图谱统计数据
     * 
     * @return 统计信息Map，包含节点数、关系数、标签类型等
     * @throws BusinessException 当获取统计信息失败时抛出 */
    @Transactional(readOnly = true)
    public Map<String, Object> getGraphStatistics() {
        try {
            return knowledgeGraphDomainService.getGraphStatistics();
        } catch (Exception e) {
            logger.error("Failed to get graph statistics: {}", e.getMessage(), e);
            throw new BusinessException("获取统计信息失败: " + e.getMessage(), e);
        }
    }
}