package org.xhy.interfaces.rest.knowledgeGraph;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.application.knowledgeGraph.schema.GraphSchemaManager;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.interfaces.api.common.Result;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱管理REST API控制器
 * 提供图谱模式管理和系统监控接口
 * 
 * @author zang
 */
@Tag(name = "Knowledge Graph Admin API", description = "知识图谱管理接口")
@RestController
@RequestMapping("/api/v1/graph/admin")
public class GraphAdminController {

    private static final Logger logger = LoggerFactory.getLogger(GraphAdminController.class);

    private final GraphSchemaManager schemaManager;

    public GraphAdminController(GraphSchemaManager schemaManager) {
        this.schemaManager = schemaManager;
    }

    /**
     * 获取所有索引信息
     */
    @Operation(summary = "获取所有索引", description = "获取知识图谱中的所有索引信息")
    @GetMapping("/indexes")
    public Result<List<Map<String, Object>>> getAllIndexes() {
        try {
            logger.info("获取所有索引信息请求");
            List<Map<String, Object>> indexes = schemaManager.getAllIndexes();
            return Result.success(indexes);
        } catch (Exception e) {
            logger.error("获取索引信息失败: {}", e.getMessage(), e);
            return Result.serverError("获取索引信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取索引统计信息
     */
    @Operation(summary = "获取索引统计", description = "获取知识图谱索引的统计信息")
    @GetMapping("/indexes/statistics")
    public Result<Map<String, Object>> getIndexStatistics() {
        try {
            logger.info("获取索引统计信息请求");
            Map<String, Object> statistics = schemaManager.getIndexStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            logger.error("获取索引统计失败: {}", e.getMessage(), e);
            return Result.serverError("获取索引统计失败: " + e.getMessage());
        }
    }

    /**
     * 创建新索引
     */
    @Operation(summary = "创建索引", description = "为指定标签和属性创建新索引")
    @PostMapping("/indexes")
    public Result<Map<String, Object>> createIndex(
            @Parameter(description = "索引定义", required = true)
            @RequestBody GraphSchemaManager.IndexDefinition indexDefinition) {
        
        try {
            logger.info("创建索引请求: {}.{}", indexDefinition.getLabel(), indexDefinition.getProperty());
            
            boolean created = schemaManager.createIndexIfNotExists(indexDefinition);
            
            Map<String, Object> response = Map.of(
                "created", created,
                "label", indexDefinition.getLabel(),
                "property", indexDefinition.getProperty(),
                "message", created ? "索引创建成功" : "索引已存在"
            );
            
            return Result.success(response);
            
        } catch (Exception e) {
            logger.error("创建索引失败: {}", e.getMessage(), e);
            return Result.serverError("创建索引失败: " + e.getMessage());
        }
    }

    /**
     * 删除指定索引
     */
    @Operation(summary = "删除索引", description = "删除指定名称的索引")
    @DeleteMapping("/indexes/{indexName}")
    public Result<Map<String, Object>> dropIndex(
            @Parameter(description = "索引名称", required = true)
            @PathVariable String indexName) {
        
        try {
            logger.info("删除索引请求: {}", indexName);
            
            schemaManager.dropIndex(indexName);
            
            Map<String, Object> response = Map.of(
                "deleted", true,
                "indexName", indexName,
                "message", "索引删除成功"
            );
            
            return Result.success(response);
            
        } catch (Exception e) {
            logger.error("删除索引失败: {}", e.getMessage(), e);
            return Result.serverError("删除索引失败: " + e.getMessage());
        }
    }

    /**
     * 重新初始化图谱模式
     */
    @Operation(summary = "重新初始化模式", description = "重新执行图谱模式初始化，创建所有配置的索引")
    @PostMapping("/schema/reinitialize")
    public Result<Map<String, Object>> reinitializeSchema() {
        try {
            logger.info("重新初始化图谱模式请求");
            
            // 获取初始化前的索引统计
            Map<String, Object> beforeStats = schemaManager.getIndexStatistics();
            
            // 重新初始化模式
            schemaManager.initializeSchema();
            
            // 获取初始化后的索引统计
            Map<String, Object> afterStats = schemaManager.getIndexStatistics();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "图谱模式重新初始化完成",
                "beforeIndexCount", beforeStats.get("totalIndexes"),
                "afterIndexCount", afterStats.get("totalIndexes")
            );
            
            return Result.success(response);
            
        } catch (Exception e) {
            logger.error("重新初始化图谱模式失败: {}", e.getMessage(), e);
            return Result.serverError("重新初始化失败: " + e.getMessage());
        }
    }

    /**
     * 全局异常处理
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        logger.error("管理接口业务异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.badRequest(e.getMessage()));
    }

    /**
     * 通用异常处理
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGenericException(Exception e) {
        logger.error("管理接口系统异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.serverError("系统错误，请联系管理员"));
    }
}