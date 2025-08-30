package org.xhy.infrastructure.knowledgeGraph.repository;

import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionResponse;
import org.xhy.application.knowledgeGraph.dto.GraphQueryRequest;
import org.xhy.application.knowledgeGraph.dto.GraphQueryResponse;
import org.xhy.application.knowledgeGraph.query.CypherQueryBuilder;
import org.xhy.domain.knowledgeGraph.repository.KnowledgeGraphRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.neo4j.util.Neo4jValueConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识图谱仓储实现类
 * 基于Neo4j实现知识图谱数据的持久化操作
 * 
 * @author zang
 * @since 1.0.0
 */
@Repository
public class KnowledgeGraphRepositoryImpl implements KnowledgeGraphRepository {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphRepositoryImpl.class);

    private final Neo4jClient neo4jClient;
    private final CypherQueryBuilder cypherQueryBuilder;

    /**
     * 构造函数
     * 
     * @param neo4jClient Neo4j客户端
     * @param cypherQueryBuilder Cypher查询构建器
     */
    public KnowledgeGraphRepositoryImpl(Neo4jClient neo4jClient, CypherQueryBuilder cypherQueryBuilder) {
        this.neo4jClient = neo4jClient;
        this.cypherQueryBuilder = cypherQueryBuilder;
    }

    /**
     * 摄取知识图谱数据
     * 批量存储实体和关系数据到Neo4j图数据库
     * 
     * @param request 图数据摄取请求，包含实体和关系数据
     * @return 摄取结果响应，包含处理统计信息
     */
    @Override
    @Transactional("neo4jTransactionManager")
    public GraphIngestionResponse ingestGraphData(GraphIngestionRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            logger.debug("Start ingesting graph data for document: {}", request.getDocumentId());

            // 检查是否有数据要处理
            boolean hasEntities = request.getEntities() != null && !request.getEntities().isEmpty();
            boolean hasRelationships = request.getRelationships() != null && !request.getRelationships().isEmpty();
            
            if (!hasEntities && !hasRelationships) {
                return GraphIngestionResponse.success(request.getDocumentId(), 0, 0, "无数据需要摄取");
            }

            // 批量摄取实体
            int entitiesProcessed = hasEntities ? ingestEntities(request.getEntities()) : 0;

            // 批量摄取关系
            int relationshipsProcessed = hasRelationships ? ingestRelationships(request.getRelationships()) : 0;

            // 创建响应
            GraphIngestionResponse response = GraphIngestionResponse.success(
                    request.getDocumentId(), entitiesProcessed, relationshipsProcessed);
            response.setProcessingTime(startTime);

            logger.debug("Graph data ingestion completed for document: {}, processing time: {}ms", 
                    request.getDocumentId(), response.getProcessingTimeMs());

            return response;

        } catch (Exception e) {
            logger.error("Graph data ingestion failed for document: {}, error: {}", 
                    request.getDocumentId(), e.getMessage(), e);
            throw new BusinessException("图数据摄取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行知识图谱查询
     * 根据查询条件执行图遍历查询
     * 
     * @param request 图查询请求，包含查询条件和返回定义
     * @return 查询结果响应，包含节点和关系数据
     */
    @Override
    @Transactional(value = "neo4jTransactionManager", readOnly = true)
    public GraphQueryResponse executeQuery(GraphQueryRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Executing graph query with {} start nodes, {} traversals, {} filters", 
                    request.getStartNodes() != null ? request.getStartNodes().size() : 0,
                    request.getTraversals() != null ? request.getTraversals().size() : 0,
                    request.getFilters() != null ? request.getFilters().size() : 0);

            // 1. 构建Cypher查询
            CypherQueryBuilder.QueryResult queryResult = cypherQueryBuilder.buildQuery(request);
            
            // 2. 执行查询
            List<Map<String, Object>> rawResults = new ArrayList<>(neo4jClient.query(queryResult.getCypher())
                    .bindAll(queryResult.getParameters())
                    .fetch()
                    .all());

            logger.debug("Query returned {} raw results", rawResults.size());

            // 3. 映射结果
            List<GraphQueryResponse.NodeResult> nodes = new ArrayList<>();
            List<GraphQueryResponse.RelationshipResult> relationships = new ArrayList<>();
            
            mapQueryResults(rawResults, nodes, relationships, request.getReturnDefinition());

            // 4. 创建响应
            GraphQueryResponse response = GraphQueryResponse.success(nodes, relationships);
            response.setExecutionTime(startTime);

            logger.debug("Graph query execution completed, nodes: {}, relationships: {}, execution time: {}ms", 
                    nodes.size(), relationships.size(), response.getExecutionTimeMs());

            return response;

        } catch (Exception e) {
            logger.error("Graph query execution failed: {}", e.getMessage(), e);
            throw new BusinessException("查询执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查找两个节点之间的路径
     * 使用最短路径算法查找节点间的连接路径
     * 
     * @param sourceNodeId 源节点ID
     * @param targetNodeId 目标节点ID
     * @param maxDepth 最大搜索深度
     * @return 查询结果响应，包含路径上的节点和关系
     */
    @Override
    @Transactional(value = "neo4jTransactionManager", readOnly = true)
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
            logger.error("Path query failed: {}", e.getMessage(), e);
            throw new BusinessException("路径查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取知识图谱统计信息
     * 统计图中的节点数量、关系数量、标签类型等信息
     * 
     * @return 统计信息Map，包含各项统计数据
     */
    @Override
    @Transactional(value = "neo4jTransactionManager", readOnly = true)
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
            logger.error("Failed to get graph statistics: {}", e.getMessage(), e);
            throw new BusinessException("获取统计信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查节点是否存在
     * 根据节点ID验证节点是否在图中存在
     * 
     * @param nodeId 节点ID
     * @return true如果节点存在，false如果不存在
     */
    @Override
    @Transactional(value = "neo4jTransactionManager", readOnly = true)
    public boolean existsNode(String nodeId) {
        try {
            String existsQuery = "MATCH (n:GenericNode {id: $nodeId}) RETURN count(n) > 0 AS exists";
            
            Map<String, Object> result = neo4jClient.query(existsQuery)
                    .bind(nodeId).to("nodeId")
                    .fetch()
                    .one()
                    .orElse(Map.of("exists", false));

            return (Boolean) result.getOrDefault("exists", false);

        } catch (Exception e) {
            logger.error("Failed to check node existence for ID: {}, error: {}", nodeId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 删除指定文档的所有图数据
     * 清理与指定文档相关的所有节点和关系
     * 
     * @param documentId 文档ID
     * @return 删除的节点数量和关系数量
     */
    @Override
    @Transactional("neo4jTransactionManager")
    public Map<String, Integer> deleteGraphDataByDocument(String documentId) {
        try {
            // 先统计要删除的数据
            String countQuery = """
                    MATCH (n:GenericNode)
                    WHERE n.documentId = $documentId
                    OPTIONAL MATCH (n)-[r]-()
                    RETURN count(DISTINCT n) AS nodeCount, count(DISTINCT r) AS relationshipCount
                    """;

            Map<String, Object> countResult = neo4jClient.query(countQuery)
                    .bind(documentId).to("documentId")
                    .fetch()
                    .one()
                    .orElse(Map.of("nodeCount", 0L, "relationshipCount", 0L));

            // 执行删除操作
            String deleteQuery = """
                    MATCH (n:GenericNode)
                    WHERE n.documentId = $documentId
                    DETACH DELETE n
                    """;

            neo4jClient.query(deleteQuery)
                    .bind(documentId).to("documentId")
                    .run();

            Map<String, Integer> result = new HashMap<>();
            result.put("nodes", ((Number) countResult.get("nodeCount")).intValue());
            result.put("relationships", ((Number) countResult.get("relationshipCount")).intValue());

            return result;

        } catch (Exception e) {
            logger.error("Failed to delete graph data for document: {}, error: {}", documentId, e.getMessage(), e);
            throw new BusinessException("删除图数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量摄取实体
     * 使用UNWIND和MERGE实现高效的批量插入/更新
     * 
     * @param entities 实体列表
     * @return 处理的实体数量
     */
    private int ingestEntities(List<GraphIngestionRequest.EntityDto> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }

        // 准备实体参数，并转换为Neo4j支持的类型
        List<Map<String, Object>> entityParams = entities.stream()
                .map(entity -> Map.of(
                        "id", entity.getId(),
                        "labels", entity.getLabels(),
                        "properties", Neo4jValueConverter.convertProperties(entity.getProperties())
                ))
                .collect(Collectors.toList());

        // 构建Cypher查询（不使用APOC，使用标准Cypher）
        String entityQuery = """
                UNWIND $entities AS entity
                MERGE (n:GenericNode {id: entity.id})
                SET n += entity.properties
                RETURN count(n) as processedCount
                """;

        try {
            logger.debug("Ingesting {} entities", entities.size());
            
            neo4jClient.query(entityQuery)
                    .bindAll(Map.of("entities", entityParams))
                    .run();

            logger.debug("Entity ingestion completed");
            return entities.size();

        } catch (Exception e) {
            logger.error("Entity ingestion failed: {}", e.getMessage(), e);
            throw new BusinessException("实体数据摄取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量摄取关系
     * 使用UNWIND和MERGE实现高效的关系创建
     * 
     * @param relationships 关系列表
     * @return 处理的关系数量
     */
    private int ingestRelationships(List<GraphIngestionRequest.RelationshipDto> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return 0;
        }

        // 准备关系参数，并转换为Neo4j支持的类型
        List<Map<String, Object>> relationshipParams = relationships.stream()
                .map(rel -> Map.of(
                        "sourceId", rel.getSourceId(),
                        "targetId", rel.getTargetId(),
                        "type", rel.getType(),
                        "properties", Neo4jValueConverter.convertProperties(
                                rel.getProperties() != null ? rel.getProperties() : Map.of())
                ))
                .collect(Collectors.toList());

        // 构建Cypher查询（使用标准Cypher，不依赖APOC）
        String relationshipQuery = """
                UNWIND $relationships AS rel
                MATCH (source:GenericNode {id: rel.sourceId})
                MATCH (target:GenericNode {id: rel.targetId})
                CREATE (source)-[r:GENERIC_RELATIONSHIP]->(target)
                SET r = rel.properties
                SET r.type = rel.type
                RETURN count(r) as processedCount
                """;

        try {
            logger.debug("Ingesting {} relationships", relationships.size());
            
            neo4jClient.query(relationshipQuery)
                    .bindAll(Map.of("relationships", relationshipParams))
                    .run();

            logger.debug("Relationship ingestion completed");
            return relationships.size();

        } catch (Exception e) {
            logger.error("Relationship ingestion failed: {}", e.getMessage(), e);
            throw new BusinessException("关系数据摄取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 映射查询结果
     * 
     * @param rawResults 原始查询结果
     * @param nodes 节点结果列表
     * @param relationships 关系结果列表
     * @param returnDef 返回定义
     */
    private void mapQueryResults(List<Map<String, Object>> rawResults, 
                               List<GraphQueryResponse.NodeResult> nodes,
                               List<GraphQueryResponse.RelationshipResult> relationships,
                               GraphQueryRequest.ReturnDefinition returnDef) {
        
        boolean includeNodes = returnDef == null || returnDef.isIncludeNodes();
        boolean includeRelationships = returnDef == null || returnDef.isIncludeRelationships();

        for (Map<String, Object> record : rawResults) {
            for (Map.Entry<String, Object> entry : record.entrySet()) {
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
     * 
     * @param node Neo4j节点
     * @return 节点结果DTO
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
     * 
     * @param relationship Neo4j关系
     * @return 关系结果DTO
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
     * 
     * @param value Neo4j值
     * @return 转换后的Java对象
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
     * 
     * @param nodes 节点列表
     * @param nodeId 节点ID
     * @return true如果包含，false如果不包含
     */
    private boolean containsNode(List<GraphQueryResponse.NodeResult> nodes, String nodeId) {
        return nodes.stream().anyMatch(node -> nodeId.equals(node.getId()));
    }
}
