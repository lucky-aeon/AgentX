package org.xhy.interfaces.rest.knowledgeGraph;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.interfaces.api.common.Result;

import java.util.Map;

/**
 * 知识图谱API使用说明控制器
 * 提供API使用文档和示例
 * 
 * @author zang
 */
@Tag(name = "Knowledge Graph API Documentation", description = "知识图谱API使用说明")
@RestController
@RequestMapping("/v1/graph/docs")
public class GraphDocumentationController {

    private static final Logger logger = LoggerFactory.getLogger(GraphDocumentationController.class);

    /**
     * 获取API使用说明
     */
    @Operation(summary = "获取API使用说明", description = "获取知识图谱API的详细使用说明和示例")
    @GetMapping("/usage")
    public Result<Map<String, Object>> getUsageDocumentation() {
        
        Map<String, Object> documentation = Map.of(
            "title", "AgentX知识图谱API使用指南",
            "version", "v1.0",
            "description", "基于Neo4j的动态知识图谱服务API",
            
            "authentication", Map.of(
                "type", "API Key",
                "header", "X-Graph-API-Key",
                "description", "在请求头中添加API密钥进行认证",
                "example", "X-Graph-API-Key: your-api-key-here"
            ),
            
            "endpoints", Map.of(
                "ingest", Map.of(
                    "method", "POST",
                    "path", "/v1/graph/ingest",
                    "description", "批量导入实体和关系数据到知识图谱",
                    "example", "curl -X POST -H 'X-Graph-API-Key: your-key' -H 'Content-Type: application/json' -d '{\"documentId\":\"doc1\",\"entities\":[...]}' /api/v1/graph/ingest"
                ),
                
                "query", Map.of(
                    "method", "POST", 
                    "path", "/v1/graph/query",
                    "description", "根据条件动态查询知识图谱中的节点和关系"
                ),
                
                "nodes", Map.of(
                    "method", "GET",
                    "path", "/v1/graph/nodes",
                    "description", "根据属性查找节点",
                    "parameters", Map.of(
                        "property", "属性名",
                        "value", "属性值", 
                        "label", "节点标签(可选)",
                        "limit", "结果限制(默认100)"
                    )
                ),
                
                "relationships", Map.of(
                    "method", "GET",
                    "path", "/v1/graph/relationships", 
                    "description", "查找节点的关系",
                    "parameters", Map.of(
                        "nodeId", "节点ID",
                        "relationshipType", "关系类型(可选)",
                        "direction", "关系方向: OUTGOING/INCOMING/BOTH",
                        "limit", "结果限制(默认100)"
                    )
                ),
                
                "path", Map.of(
                    "method", "GET",
                    "path", "/v1/graph/path",
                    "description", "查找两个节点之间的路径", 
                    "parameters", Map.of(
                        "sourceNodeId", "源节点ID",
                        "targetNodeId", "目标节点ID",
                        "maxDepth", "最大路径深度(默认5)"
                    )
                ),
                
                "statistics", Map.of(
                    "method", "GET",
                    "path", "/v1/graph/statistics",
                    "description", "获取知识图谱的统计信息"
                ),
                
                "health", Map.of(
                    "method", "GET", 
                    "path", "/v1/graph/health",
                    "description", "健康检查端点(无需认证)"
                )
            ),
            
            "administration", Map.of(
                "description", "管理员接口需要相同的API密钥认证",
                "endpoints", Map.of(
                    "indexes", "GET /v1/graph/admin/indexes - 获取所有索引信息",
                    "createIndex", "POST /v1/graph/admin/indexes - 创建新索引",
                    "dropIndex", "DELETE /v1/graph/admin/indexes/{indexName} - 删除索引",
                    "reinitialize", "POST /v1/graph/admin/schema/reinitialize - 重新初始化模式"
                )
            ),
            
            "configuration", Map.of(
                "security", Map.of(
                    "enabled", "agentx.graph.security.enabled (默认: true)",
                    "apiKey", "agentx.graph.security.api-key",
                    "headerName", "agentx.graph.security.header-name (默认: X-Graph-API-Key)"
                ),
                "schema", Map.of(
                    "autoCreate", "agentx.graph.schema.auto-create-indexes (默认: true)",
                    "initialIndexes", "agentx.graph.schema.initial-indexes - 初始索引列表"
                )
            ),
            
            "examples", Map.of(
                "ingestion", Map.of(
                    "documentId", "doc_001",
                    "entities", "[{\"id\":\"person_1\",\"labels\":[\"人物\"],\"properties\":{\"name\":\"胡展鸿\",\"age\":25}}]",
                    "relationships", "[{\"sourceId\":\"person_1\",\"targetId\":\"skill_1\",\"type\":\"掌握\",\"properties\":{\"level\":\"高级\"}}]"
                ),
                "query", Map.of(
                    "startNodes", "[{\"label\":\"人物\",\"property\":\"name\",\"value\":\"胡展鸿\",\"operator\":\"eq\"}]",
                    "traversals", "[{\"relationshipType\":\"掌握\",\"direction\":\"OUTGOING\"}]",
                    "returnDefinition", "{\"includeNodes\":true,\"includeRelationships\":true}"
                )
            )
        );
        
        return Result.success(documentation);
    }

    /**
     * 获取API配置示例
     */
    @Operation(summary = "获取配置示例", description = "获取知识图谱服务的配置示例")
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfigurationExamples() {
        
        Map<String, Object> config = Map.of(
            "application.yml", """
                agentx:
                  graph:
                    security:
                      enabled: true
                      api-key: ${GRAPH_API_KEY:CHANGE-ME-IN-PRODUCTION}
                      header-name: X-Graph-API-Key
                    schema:
                      auto-create-indexes: true
                      initial-indexes:
                        - label: "人物"
                          property: "id"
                        - label: "技术"
                          property: "name"
                    ingestion:
                      batch-size: 1000
                      timeout: 60s
                    query:
                      default-limit: 100
                      max-limit: 10000
                """,
            
            "environment_variables", Map.of(
                "GRAPH_API_KEY", "设置API密钥",
                "GRAPH_SECURITY_ENABLED", "启用/禁用安全认证(true/false)",
                "NEO4J_URL", "Neo4j数据库连接URL",
                "NEO4J_USERNAME", "Neo4j用户名",
                "NEO4J_PASSWORD", "Neo4j密码"
            ),
            
            "docker_example", """
                docker run -d \\
                  -e GRAPH_API_KEY=your-secure-api-key \\
                  -e NEO4J_URL=bolt://neo4j-server:7687 \\
                  -e NEO4J_USERNAME=neo4j \\
                  -e NEO4J_PASSWORD=password \\
                  -p 8080:8080 \\
                  agentx-knowledge-graph
                """
        );
        
        return Result.success(config);
    }
}