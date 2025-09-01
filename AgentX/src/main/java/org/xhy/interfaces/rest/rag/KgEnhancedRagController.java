package org.xhy.interfaces.rest.rag;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.rag.dto.KgEnhancedRagRequest;
import org.xhy.application.rag.dto.KgEnhancedRagResponse;
import org.xhy.application.rag.service.KnowledgeGraphEnhancedRagService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

/** 知识图谱增强RAG检索REST API控制器 提供基于知识图谱增强的RAG检索服务
 *
 * @author AgentX */
@Tag(name = "Knowledge Graph Enhanced RAG API", description = "知识图谱增强RAG检索接口")
@RestController
@RequestMapping("/api/v1/rag/kg-enhanced")
@Validated
public class KgEnhancedRagController {

    private static final Logger log = LoggerFactory.getLogger(KgEnhancedRagController.class);

    private final KnowledgeGraphEnhancedRagService kgEnhancedRagService;
    public KgEnhancedRagController(KnowledgeGraphEnhancedRagService kgEnhancedRagService) {
        this.kgEnhancedRagService = kgEnhancedRagService;
    }

    /** 执行知识图谱增强的RAG检索
     *
     * @param request 增强RAG检索请求
     * @return 增强RAG检索响应 */
    @Operation(summary = "知识图谱增强RAG检索", description = "结合向量搜索和知识图谱查询，提供更准确和全面的检索结果")
    @PostMapping("/search")
    public Result<KgEnhancedRagResponse> enhancedRagSearch(
            @Parameter(description = "知识图谱增强RAG检索请求", required = true) @RequestBody @Valid KgEnhancedRagRequest request) {

        try {
            // 获取当前用户ID
            String userId = UserContext.getCurrentUserId();

            log.info("收到知识图谱增强RAG检索请求，用户: {}, 查询: '{}', 数据集数量: {}", userId, request.getQuestion(),
                    request.getDatasetIds() != null ? request.getDatasetIds().size() : 0);

            // 执行增强RAG检索
            KgEnhancedRagResponse response = kgEnhancedRagService.enhancedRagSearch(request, userId);

            log.info("知识图谱增强RAG检索完成，用户: {}, 返回结果数: {}, 处理时间: {}ms", userId,
                    response.getResults() != null ? response.getResults().size() : 0, response.getProcessingTimeMs());

            return Result.success(response);

        } catch (Exception e) {
            log.error("知识图谱增强RAG检索失败", e);
            return Result.serverError("检索失败: " + e.getMessage());
        }
    }

    /** 获取增强RAG检索的默认配置
     *
     * @return 默认配置 */
    @Operation(summary = "获取默认配置", description = "获取知识图谱增强RAG检索的默认参数配置")
    @GetMapping("/default-config")
    public Result<KgEnhancedRagRequest> getDefaultConfig() {
        try {
            KgEnhancedRagRequest defaultConfig = new KgEnhancedRagRequest();
            // 设置默认值已经在DTO中定义

            return Result.success(defaultConfig);

        } catch (Exception e) {
            log.error("获取默认配置失败", e);
            return Result.serverError("检索失败: " + e.getMessage());
        }
    }

}
