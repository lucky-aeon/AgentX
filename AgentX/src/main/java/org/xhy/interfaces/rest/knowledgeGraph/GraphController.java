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
import org.xhy.application.knowledgeGraph.service.GraphIngestionService;
import org.xhy.application.knowledgeGraph.service.GraphQueryService;
import org.xhy.application.knowledgeGraph.service.GenerateGraphService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.api.common.Result;

import java.util.Map;

/**
 * 知识图谱REST API控制器
 * 提供图数据摄取和查询的RESTful接口
 * 
 * @author zang
 */
@Tag(name = "Knowledge Graph API", description = "动态知识图谱服务接口")
@RestController
@RequestMapping("/v1/graph")
@Validated
public class GraphController {

    private static final Logger logger = LoggerFactory.getLogger(GraphController.class);

    private final GraphIngestionService ingestionService;
    private final GraphQueryService queryService;
    private final GenerateGraphService generateGraphService;

    public GraphController(GraphIngestionService ingestionService, GraphQueryService queryService, GenerateGraphService generateGraphService) {
        this.ingestionService = ingestionService;
        this.queryService = queryService;
        this.generateGraphService = generateGraphService;
    }

    /**
     * 摄取图数据
     * 
     * @param request 图数据摄取请求
     * @return 摄取结果
     */
    @Operation(summary = "摄取图数据", description = "批量导入实体和关系数据到知识图谱")
    @PostMapping("/ingest")
    public Result<GraphIngestionResponse> ingestGraphData(
            @Parameter(description = "图数据摄取请求", required = true)
            @RequestBody @Valid GraphIngestionRequest request) {
        
        logger.info("收到图数据摄取请求，文档ID: {}, 实体数量: {}, 关系数量: {}", 
                request.getDocumentId(), 
                request.getEntities() != null ? request.getEntities().size() : 0,
                request.getRelationships() != null ? request.getRelationships().size() : 0);

        GraphIngestionResponse response = ingestionService.ingestGraphData(request);
        return Result.success(response);
    }

    /**
     * 生成知识图谱
     *
     * @param request 图谱生成请求
     * @return 生成结果
     */
    @Operation(summary = "生成知识图谱", description = "根据文件ID生成知识图谱，支持异步处理")
    @PostMapping("/generate")
    public Result<GraphGenerateResponse> generateGraph(
            @Parameter(description = "图谱生成请求", required = true)
            @RequestBody @Valid GraphGenerateRequest request) {
        
        logger.info("收到图谱生成请求，文件ID: {}, 异步处理: {}", 
                request.getFileId(), request.getAsync());

        try {
            // 调用服务生成图谱
            String documentText = generateGraphService.generateGraph(request.getFileId());
            
            // 生成任务ID（这里可以根据实际需要生成更复杂的任务ID）
            String taskId = "graph_" + request.getFileId() + "_" + System.currentTimeMillis();
            
            // 构建响应
            GraphGenerateResponse response;
            if (request.getAsync()) {
                // 异步处理模式
                response = GraphGenerateResponse.processing(
                    taskId, 
                    "知识图谱生成任务已提交，正在后台处理中", 
                    documentText.length() > 200 ? documentText.substring(0, 200) + "..." : documentText
                );
            } else {
                // 同步处理模式
                response = GraphGenerateResponse.completed(
                    taskId, 
                    "知识图谱生成完成"
                );
                response.setDocumentPreview(
                    documentText.length() > 200 ? documentText.substring(0, 200) + "..." : documentText
                );
            }
            
            return Result.success(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("图谱生成请求参数错误: {}", e.getMessage());
            GraphGenerateResponse errorResponse = GraphGenerateResponse.failed(
                "error_" + System.currentTimeMillis(),
                e.getMessage()
            );
            return Result.badRequest(e.getMessage());
            
        } catch (Exception e) {
            logger.error("图谱生成失败: {}", e.getMessage(), e);
            GraphGenerateResponse errorResponse = GraphGenerateResponse.failed(
                "error_" + System.currentTimeMillis(),
                "图谱生成失败: " + e.getMessage()
            );
            return Result.serverError( "图谱生成过程中发生错误");
        }
    }

    /**
     * 动态查询图数据
     * 
     * @param request 图查询请求
     * @return 查询结果
     */
    @Operation(summary = "动态查询图数据", description = "根据条件动态查询知识图谱中的节点和关系")
    @PostMapping("/query")
    public Result<GraphQueryResponse> queryGraph(
            @Parameter(description = "图查询请求", required = true)
            @RequestBody @Valid GraphQueryRequest request) {
        
        logger.info("收到图查询请求，起始节点数量: {}", 
                request.getStartNodes() != null ? request.getStartNodes().size() : 0);

        GraphQueryResponse response = queryService.executeQuery(request);
        return Result.success(response);
    }

    /**
     * 根据属性查找节点
     * 
     * @param label 节点标签
     * @param property 属性名
     * @param value 属性值
     * @param limit 结果限制
     * @return 查询结果
     */
    @Operation(summary = "根据属性查找节点", description = "根据标签和属性值查找匹配的节点")
    @GetMapping("/nodes")
    public Result<GraphQueryResponse> findNodesByProperty(
            @Parameter(description = "节点标签", example = "人物")
            @RequestParam(required = false) String label,
            @Parameter(description = "属性名", example = "name")
            @RequestParam String property,
            @Parameter(description = "属性值", example = "胡展鸿")
            @RequestParam String value,
            @Parameter(description = "结果限制", example = "100")
            @RequestParam(defaultValue = "100") Integer limit) {

        logger.info("查找节点请求，标签: {}, 属性: {} = {}", label, property, value);

        GraphQueryResponse response = queryService.findNodesByProperty(label, property, value, limit);
        return Result.success(response);
    }

    /**
     * 查找节点的关系
     * 
     * @param nodeId 节点ID
     * @param relationshipType 关系类型
     * @param direction 关系方向
     * @param limit 结果限制
     * @return 查询结果
     */
    @Operation(summary = "查找节点关系", description = "查找指定节点的所有关系")
    @GetMapping("/relationships")
    public Result<GraphQueryResponse> findNodeRelationships(
            @Parameter(description = "节点ID", required = true)
            @RequestParam String nodeId,
            @Parameter(description = "关系类型", example = "掌握")
            @RequestParam(required = false) String relationshipType,
            @Parameter(description = "关系方向", example = "OUTGOING", schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"OUTGOING", "INCOMING", "BOTH"}))
            @RequestParam(defaultValue = "BOTH") String direction,
            @Parameter(description = "结果限制", example = "100")
            @RequestParam(defaultValue = "100") Integer limit) {

        logger.info("查找节点关系请求，节点ID: {}, 关系类型: {}, 方向: {}", nodeId, relationshipType, direction);

        GraphQueryResponse response = queryService.findNodeRelationships(nodeId, relationshipType, direction, limit);
        return Result.success(response);
    }

    /**
     * 查找两个节点之间的路径
     * 
     * @param sourceNodeId 源节点ID
     * @param targetNodeId 目标节点ID
     * @param maxDepth 最大路径深度
     * @return 路径查询结果
     */
    @Operation(summary = "查找节点路径", description = "查找两个节点之间的最短路径")
    @GetMapping("/path")
    public Result<GraphQueryResponse> findPath(
            @Parameter(description = "源节点ID", required = true)
            @RequestParam String sourceNodeId,
            @Parameter(description = "目标节点ID", required = true)
            @RequestParam String targetNodeId,
            @Parameter(description = "最大路径深度", example = "5")
            @RequestParam(defaultValue = "5") Integer maxDepth) {

        logger.info("查找路径请求，源节点: {} -> 目标节点: {}, 最大深度: {}", 
                sourceNodeId, targetNodeId, maxDepth);

        GraphQueryResponse response = queryService.findPathBetweenNodes(sourceNodeId, targetNodeId, maxDepth);
        return Result.success(response);
    }

    /**
     * 获取图统计信息
     * 
     * @return 图统计信息
     */
    @Operation(summary = "获取图统计信息", description = "获取知识图谱的统计信息")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getGraphStatistics() {
        logger.info("获取图统计信息请求");

        Map<String, Object> statistics = queryService.getGraphStatistics();
        return Result.success(statistics);
    }

    /**
     * 健康检查端点
     * 
     * @return 服务健康状态
     */
    @Operation(summary = "健康检查", description = "检查知识图谱服务的健康状态")
    @GetMapping("/health")
    public Result<Map<String, Object>> healthCheck() {
        try {
            // 执行简单的连接测试
            Map<String, Object> statistics = queryService.getGraphStatistics();
            
            Map<String, Object> health = Map.of(
                    "status", "UP",
                    "service", "Knowledge Graph Service",
                    "timestamp", System.currentTimeMillis(),
                    "database", "Connected",
                    "totalNodes", statistics.get("totalNodes"),
                    "totalRelationships", statistics.get("totalRelationships")
            );
            
            return Result.success(health);
            
        } catch (Exception e) {
            logger.error("健康检查失败: {}", e.getMessage(), e);
            
            Map<String, Object> health = Map.of(
                    "status", "DOWN",
                    "service", "Knowledge Graph Service",
                    "timestamp", System.currentTimeMillis(),
                    "error", e.getMessage()
            );
            
            return Result.success(health);
        }
    }

    /**
     * 全局异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        logger.error("业务异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.badRequest(e.getMessage()));
    }

    /**
     * 参数校验异常处理
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleValidationException(jakarta.validation.ConstraintViolationException e) {
        logger.error("参数校验异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.badRequest("参数校验失败: " + e.getMessage()));
    }

    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGenericException(Exception e) {
        logger.error("系统异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.serverError("系统错误，请联系管理员"));
    }
}