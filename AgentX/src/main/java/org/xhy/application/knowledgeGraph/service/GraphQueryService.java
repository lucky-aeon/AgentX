package org.xhy.application.knowledgeGraph.service;

import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.knowledgeGraph.dto.GraphQueryRequest;
import org.xhy.application.knowledgeGraph.dto.GraphQueryResponse;
import org.xhy.application.knowledgeGraph.query.CypherQueryBuilder;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图查询服务
 * 负责执行动态图查询并映射结果
 * 
 * @author zang
 */
@Service
public class GraphQueryService {

    private static final Logger logger = LoggerFactory.getLogger(GraphQueryService.class);

    private final Neo4jClient neo4jClient;
    private final CypherQueryBuilder queryBuilder;

    public GraphQueryService(Neo4jClient neo4jClient, CypherQueryBuilder queryBuilder) {
        this.neo4jClient = neo4jClient;
        this.queryBuilder = queryBuilder;
    }

    /**
     * 执行图查询
     * 
     * @param request 图查询请求
     * @return 查询结果响应
     */
    @Transactional(readOnly = true)
    public GraphQueryResponse executeQuery(GraphQueryRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("开始执行图查询，起始节点数量: {}, 遍历步骤数量: {}, 过滤条件数量: {}", 
                    request.getStartNodes() != null ? request.getStartNodes().size() : 0,
                    request.getTraversals() != null ? request.getTraversals().size() : 0,
                    request.getFilters() != null ? request.getFilters().size() : 0);

            // 1. 构建Cypher查询
            CypherQueryBuilder.QueryResult queryResult = queryBuilder.buildQuery(request);
            
            // 2. 执行查询
            List<Map<String, Object>> rawResults = new ArrayList<>(neo4jClient.query(queryResult.getCypher())
                    .bindAll(queryResult.getParameters())
                    .fetch()
                    .all());

            logger.debug("查询返回原始结果数量: {}", rawResults.size());

            // 3. 映射结果
            List<GraphQueryResponse.NodeResult> nodes = new ArrayList<>();
            List<GraphQueryResponse.RelationshipResult> relationships = new ArrayList<>();
            
            mapQueryResults(rawResults, nodes, relationships, request.getReturnDefinition());

            // 4. 创建响应
            GraphQueryResponse response = GraphQueryResponse.success(nodes, relationships);
            response.setExecutionTime(startTime);

            logger.info("图查询执行完成，节点数量: {}, 关系数量: {}, 执行时间: {}ms", 
                    nodes.size(), relationships.size(), response.getExecutionTimeMs());

            return response;

        } catch (Exception e) {
            logger.error("图查询执行失败: {}", e.getMessage(), e);
            throw new BusinessException("查询执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 简单节点查询 - 根据标签和属性查找节点
     * 
     * @param label 节点标签
     * @param property 属性名
     * @param value 属性值
     * @param limit 结果限制
     * @return 查询结果
     */
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

    /**
     * 查找节点的所有关系
     * 
     * @param nodeId 节点ID
     * @param relationshipType 关系类型，null表示所有类型
     * @param direction 关系方向：OUTGOING, INCOMING, BOTH
     * @param limit 结果限制
     * @return 查询结果
     */
    @Transactional(readOnly = true)
    public GraphQueryResponse findNodeRelationships(String nodeId, String relationshipType, 
                                                   String direction, Integer limit) {
        GraphQueryRequest request = new GraphQueryRequest();
        
        // 设置起始节点
        GraphQueryRequest.NodeFilter nodeFilter = new GraphQueryRequest.NodeFilter(null, "id", nodeId);
        request.setStartNodes(List.of(nodeFilter));
        
        // 设置遍历步骤
        GraphQueryRequest.TraversalStep traversal = new GraphQueryRequest.TraversalStep(
                relationshipType != null ? relationshipType : "*", 
                direction != null ? direction : "BOTH"
        );
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

    /**
     * 路径查询 - 查找两个节点之间的路径
     * 
     * @param sourceNodeId 源节点ID
     * @param targetNodeId 目标节点ID
     * @param maxDepth 最大深度
     * @return 查询结果
     */
    @Transactional(readOnly = true)
    public GraphQueryResponse findPathBetweenNodes(String sourceNodeId, String targetNodeId, Integer maxDepth) {
        long startTime = System.currentTimeMillis();
        
        try {
            String pathQuery = """
                    MATCH (source:GenericNode {id: $sourceId}), (target:GenericNode {id: $targetId})
                    MATCH path = shortestPath((source)-[*..%d]-(target))
                    RETURN nodes(path) AS pathNodes, relationships(path) AS pathRelationships
                    """.formatted(maxDepth != null ? maxDepth : 5);

            Map<String, Object> params = Map.of(
                    "sourceId", sourceNodeId,
                    "targetId", targetNodeId
            );

            List<Map<String, Object>> rawResults = new ArrayList<>(neo4jClient.query(pathQuery)
                    .bindAll(params)
                    .fetch()
                    .all());

            List<GraphQueryResponse.NodeResult> nodes = new ArrayList<>();
            List<GraphQueryResponse.RelationshipResult> relationships = new ArrayList<>();

            for (Map<String, Object> result : rawResults) {
                @SuppressWarnings("unchecked")
                List<Node> pathNodes = (List<Node>) result.get("pathNodes");
                @SuppressWarnings("unchecked")
                List<Relationship> pathRelationships = (List<Relationship>) result.get("pathRelationships");

                if (pathNodes != null) {
                    for (Node node : pathNodes) {
                        nodes.add(mapNodeToResult(node));
                    }
                }

                if (pathRelationships != null) {
                    for (Relationship relationship : pathRelationships) {
                        relationships.add(mapRelationshipToResult(relationship));
                    }
                }
            }

            GraphQueryResponse response = GraphQueryResponse.success(nodes, relationships);
            response.setExecutionTime(startTime);

            return response;

        } catch (Exception e) {
            logger.error("路径查询失败: {}", e.getMessage(), e);
            throw new BusinessException("路径查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 映射查询结果
     */
    private void mapQueryResults(List<Map<String, Object>> rawResults, 
                               List<GraphQueryResponse.NodeResult> nodes,
                               List<GraphQueryResponse.RelationshipResult> relationships,
                               GraphQueryRequest.ReturnDefinition returnDef) {
        
        boolean includeNodes = returnDef == null || returnDef.isIncludeNodes();
        boolean includeRelationships = returnDef == null || returnDef.isIncludeRelationships();

        for (Map<String, Object> record : rawResults) {
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value == null) {
                    continue;
                }

                if (value instanceof Node && includeNodes) {
                    Node node = (Node) value;
                    GraphQueryResponse.NodeResult nodeResult = mapNodeToResult(node);
                    if (!containsNode(nodes, nodeResult.getId())) {
                        nodes.add(nodeResult);
                    }
                } else if (value instanceof Relationship && includeRelationships) {
                    Relationship relationship = (Relationship) value;
                    relationships.add(mapRelationshipToResult(relationship));
                }
            }
        }
    }

    /**
     * 映射Neo4j节点到结果DTO
     */
    private GraphQueryResponse.NodeResult mapNodeToResult(Node node) {
        String id = node.get("id").asString();
        List<String> labels = new ArrayList<>();
        node.labels().forEach(labels::add);
        
        Map<String, Object> properties = new HashMap<>();
        for (String key : node.keys()) {
            properties.put(key, convertNeo4jValue(node.get(key).asObject()));
        }

        return new GraphQueryResponse.NodeResult(id, labels, properties);
    }

    /**
     * 映射Neo4j关系到结果DTO
     */
    private GraphQueryResponse.RelationshipResult mapRelationshipToResult(Relationship relationship) {
        Long id = relationship.id();
        String type = relationship.type();
        String sourceNodeId = String.valueOf(relationship.startNodeId());
        String targetNodeId = String.valueOf(relationship.endNodeId());
        
        Map<String, Object> properties = new HashMap<>();
        for (String key : relationship.keys()) {
            properties.put(key, convertNeo4jValue(relationship.get(key).asObject()));
        }

        return new GraphQueryResponse.RelationshipResult(id, type, sourceNodeId, targetNodeId, properties);
    }

    /**
     * 转换Neo4j值类型
     */
    private Object convertNeo4jValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // Neo4j特殊类型转换 - 简化版本，避免使用不存在的类
        // 如果需要DateTime处理，可以在运行时通过反射判断
        return value;
    }

    /**
     * 检查节点列表是否包含指定ID的节点
     */
    private boolean containsNode(List<GraphQueryResponse.NodeResult> nodes, String nodeId) {
        return nodes.stream().anyMatch(node -> nodeId.equals(node.getId()));
    }

    /**
     * 获取图统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGraphStatistics() {
        try {
            String statsQuery = """
                    MATCH (n:GenericNode)
                    OPTIONAL MATCH ()-[r]->()
                    RETURN 
                        count(DISTINCT n) AS totalNodes,
                        count(DISTINCT r) AS totalRelationships,
                        collect(DISTINCT labels(n)) AS nodeLabels,
                        collect(DISTINCT type(r)) AS relationshipTypes
                    """;

            Map<String, Object> result = neo4jClient.query(statsQuery)
                    .fetch()
                    .one()
                    .orElse(new HashMap<>());

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalNodes", result.getOrDefault("totalNodes", 0L));
            stats.put("totalRelationships", result.getOrDefault("totalRelationships", 0L));
            stats.put("nodeLabels", result.getOrDefault("nodeLabels", new ArrayList<>()));
            stats.put("relationshipTypes", result.getOrDefault("relationshipTypes", new ArrayList<>()));
            stats.put("timestamp", System.currentTimeMillis());

            return stats;

        } catch (Exception e) {
            logger.error("获取图统计信息失败: {}", e.getMessage(), e);
            throw new BusinessException("获取统计信息失败: " + e.getMessage(), e);
        }
    }
}