package org.xhy.application.knowledgeGraph.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionResponse;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图数据摄取服务
 * 负责批量处理和存储图数据到Neo4j数据库
 * 
 * @author zang
 */
@Service
public class GraphIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(GraphIngestionService.class);

    private final Neo4jClient neo4jClient;

    public GraphIngestionService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    /**
     * 摄取图数据
     * 
     * @param request 图数据摄取请求
     * @return 摄取结果响应
     */
    @Transactional
    public GraphIngestionResponse ingestGraphData(GraphIngestionRequest request) {
        if (request == null) {
            throw new BusinessException("摄取请求不能为空");
        }

        if (request.getDocumentId() == null || request.getDocumentId().trim().isEmpty()) {
            throw new BusinessException("文档ID不能为空");
        }

        long startTime = System.currentTimeMillis();

        try {
            logger.info("开始图数据摄取，文档ID: {}", request.getDocumentId());

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

            logger.info("图数据摄取完成，文档ID: {}, 处理时间: {}ms", 
                    request.getDocumentId(), response.getProcessingTimeMs());

            return response;

        } catch (Exception e) {
            logger.error("图数据摄取失败，文档ID: {}, 错误信息: {}", request.getDocumentId(), e.getMessage(), e);
            throw new BusinessException("图数据摄取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量摄取实体
     * 使用UNWIND和MERGE实现高效的批量插入/更新
     */
    private int ingestEntities(List<GraphIngestionRequest.EntityDto> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }

        // 准备实体参数
        List<Map<String, Object>> entityParams = entities.stream()
                .map(entity -> Map.of(
                        "id", entity.getId(),
                        "labels", entity.getLabels(),
                        "properties", entity.getProperties()
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
            logger.debug("执行实体摄取，数量: {}", entities.size());
            
            neo4jClient.query(entityQuery)
                    .bindAll(Map.of("entities", entityParams))
                    .run();

            logger.debug("实体摄取完成");
            return entities.size();

        } catch (Exception e) {
            logger.error("实体摄取失败: {}", e.getMessage(), e);
            throw new BusinessException("实体数据摄取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量摄取关系
     * 使用UNWIND和MERGE实现高效的关系创建
     */
    private int ingestRelationships(List<GraphIngestionRequest.RelationshipDto> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return 0;
        }

        // 准备关系参数
        List<Map<String, Object>> relationshipParams = relationships.stream()
                .map(rel -> Map.of(
                        "sourceId", rel.getSourceId(),
                        "targetId", rel.getTargetId(),
                        "type", rel.getType(),
                        "properties", rel.getProperties() != null ? rel.getProperties() : Map.of()
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
            logger.debug("执行关系摄取，数量: {}", relationships.size());
            
            neo4jClient.query(relationshipQuery)
                    .bindAll(Map.of("relationships", relationshipParams))
                    .run();

            logger.debug("关系摄取完成");
            return relationships.size();

        } catch (Exception e) {
            logger.error("关系摄取失败: {}", e.getMessage(), e);
            throw new BusinessException("关系数据摄取失败: " + e.getMessage(), e);
        }
    }
}