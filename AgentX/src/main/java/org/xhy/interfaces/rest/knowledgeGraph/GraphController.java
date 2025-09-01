package org.xhy.interfaces.rest.knowledgeGraph;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionResponse;
import org.xhy.application.knowledgeGraph.dto.GraphQueryRequest;
import org.xhy.application.knowledgeGraph.dto.GraphQueryResponse;
import org.xhy.application.knowledgeGraph.dto.GraphGenerateRequest;
import org.xhy.application.knowledgeGraph.dto.GraphGenerateResponse;
import org.xhy.application.knowledgeGraph.service.GraphIngestionAppService;
import org.xhy.application.knowledgeGraph.service.GraphQueryAppService;
import org.xhy.application.knowledgeGraph.service.KnowledgeGraphAppService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.api.common.Result;

import java.util.Map;

/** 知识图谱REST API控制器 提供图数据摄取和查询的RESTful接口
 *
 * @author zang
 * @since 1.0.0 */
@Tag(name = "Knowledge Graph API", description = "动态知识图谱服务接口")
@RestController
@RequestMapping("/v1/graph")
@Validated
public class GraphController {

    private static final Logger logger = LoggerFactory.getLogger(GraphController.class);

    private final GraphIngestionAppService graphIngestionAppService;
    private final GraphQueryAppService graphQueryAppService;

    /** 构造函数
     * 
     * @param graphIngestionAppService 图数据摄取应用服务
     * @param graphQueryAppService 图查询应用服务 */
    public GraphController(GraphIngestionAppService graphIngestionAppService,
            GraphQueryAppService graphQueryAppService) {
        this.graphIngestionAppService = graphIngestionAppService;
        this.graphQueryAppService = graphQueryAppService;
    }
    /** 摄取图数据 批量导入实体和关系数据到知识图谱
     *
     * @param request 图数据摄取请求
     * @return 摄取结果响应 */
    @Operation(summary = "摄取图数据", description = "批量导入实体和关系数据到知识图谱")
    @PostMapping("/ingest")
    public Result<GraphIngestionResponse> ingestGraphData(
            @Parameter(description = "图数据摄取请求", required = true) @RequestBody @Validated GraphIngestionRequest request) {

        logger.info("Received graph data ingestion request, document ID: {}, entities: {}, relationships: {}",
                request.getDocumentId(), request.getEntities() != null ? request.getEntities().size() : 0,
                request.getRelationships() != null ? request.getRelationships().size() : 0);

        GraphIngestionResponse response = graphIngestionAppService.ingestGraphData(request);
        return Result.success(response);
    }
    /** 动态查询图数据 根据条件动态查询知识图谱中的节点和关系
     *
     * @param request 图查询请求
     * @return 查询结果响应 */
    @Operation(summary = "动态查询图数据", description = "根据条件动态查询知识图谱中的节点和关系")
    @PostMapping("/query")
    public Result<GraphQueryResponse> queryGraph(
            @Parameter(description = "图查询请求", required = true) @RequestBody @Validated GraphQueryRequest request) {

        logger.info("Received graph query request, start nodes count: {}",
                request.getStartNodes() != null ? request.getStartNodes().size() : 0);

        GraphQueryResponse response = graphQueryAppService.executeQuery(request);
        return Result.success(response);
    }
    /** 根据属性查找节点 根据标签和属性值查找匹配的节点
     *
     * @param label 节点标签，可选
     * @param property 属性名
     * @param value 属性值
     * @param limit 结果限制
     * @return 查询结果响应 */
    @Operation(summary = "根据属性查找节点", description = "根据标签和属性值查找匹配的节点")
    @GetMapping("/nodes")
    public Result<GraphQueryResponse> findNodesByProperty(
            @Parameter(description = "节点标签", example = "人物") @RequestParam(required = false) String label,
            @Parameter(description = "属性名", example = "name") @RequestParam String property,
            @Parameter(description = "属性值", example = "胡展鸿") @RequestParam String value,
            @Parameter(description = "结果限制", example = "100") @RequestParam(defaultValue = "100") Integer limit) {

        logger.info("Find nodes request, label: {}, property: {} = {}", label, property, value);

        GraphQueryResponse response = graphQueryAppService.findNodesByProperty(label, property, value, limit);
        return Result.success(response);
    }

    /** 查找节点的关系 查找指定节点的所有关系
     *
     * @param nodeId 节点ID
     * @param relationshipType 关系类型，可选
     * @param direction 关系方向
     * @param limit 结果限制
     * @return 查询结果响应 */
    @Operation(summary = "查找节点关系", description = "查找指定节点的所有关系")
    @GetMapping("/relationships")
    public Result<GraphQueryResponse> findNodeRelationships(
            @Parameter(description = "节点ID", required = true) @RequestParam String nodeId,
            @Parameter(description = "关系类型", example = "掌握") @RequestParam(required = false) String relationshipType,
            @Parameter(description = "关系方向", example = "OUTGOING", schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {
                    "OUTGOING", "INCOMING", "BOTH"})) @RequestParam(defaultValue = "BOTH") String direction,
            @Parameter(description = "结果限制", example = "100") @RequestParam(defaultValue = "100") Integer limit) {

        logger.info("Find node relationships request, node ID: {}, relationship type: {}, direction: {}", nodeId,
                relationshipType, direction);

        GraphQueryResponse response = graphQueryAppService.findNodeRelationships(nodeId, relationshipType, direction,
                limit);
        return Result.success(response);
    }

    /** 查找两个节点之间的路径 查找两个节点之间的最短路径
     *
     * @param sourceNodeId 源节点ID
     * @param targetNodeId 目标节点ID
     * @param maxDepth 最大路径深度
     * @return 路径查询结果响应 */
    @Operation(summary = "查找节点路径", description = "查找两个节点之间的最短路径")
    @GetMapping("/path")
    public Result<GraphQueryResponse> findPath(
            @Parameter(description = "源节点ID", required = true) @RequestParam String sourceNodeId,
            @Parameter(description = "目标节点ID", required = true) @RequestParam String targetNodeId,
            @Parameter(description = "最大路径深度", example = "5") @RequestParam(defaultValue = "5") Integer maxDepth) {

        logger.info("Find path request, source node: {} -> target node: {}, max depth: {}", sourceNodeId, targetNodeId,
                maxDepth);

        GraphQueryResponse response = graphQueryAppService.findPathBetweenNodes(sourceNodeId, targetNodeId, maxDepth);
        return Result.success(response);
    }
}