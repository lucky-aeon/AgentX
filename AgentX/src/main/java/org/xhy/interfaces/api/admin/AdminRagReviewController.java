package org.xhy.interfaces.api.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.rag.RagPublishAppService;
import org.xhy.application.rag.dto.RagVersionDTO;
import org.xhy.application.rag.dto.RagStatisticsDTO;
import org.xhy.application.rag.dto.RagContentPreviewDTO;
import org.xhy.application.rag.request.ReviewRagVersionRequest;
import org.xhy.application.rag.request.BatchReviewRequest;
import org.xhy.application.rag.request.QueryRagVersionRequest;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.rag.request.QueryPendingReviewRequest;

/** 管理员RAG审核控制器
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@RestController
@RequestMapping("/admin/rags")
public class AdminRagReviewController {

    private final RagPublishAppService ragPublishAppService;

    public AdminRagReviewController(RagPublishAppService ragPublishAppService) {
        this.ragPublishAppService = ragPublishAppService;
    }

    /** 获取待审核的RAG版本列表
     * 
     * @param request 查询请求
     * @return 待审核版本列表 */
    @GetMapping("/pending")
    public Result<Page<RagVersionDTO>> getPendingReviewVersions(QueryPendingReviewRequest request) {
        Page<RagVersionDTO> result = ragPublishAppService.getPendingReviewVersions(request);
        return Result.success(result);
    }

    /** 审核RAG版本
     * 
     * @param versionId 版本ID
     * @param request 审核请求
     * @return 审核后的版本信息 */
    @PostMapping("/{versionId}")
    public Result<RagVersionDTO> reviewRagVersion(@PathVariable String versionId,
            @RequestBody @Validated ReviewRagVersionRequest request) {
        RagVersionDTO result = ragPublishAppService.reviewRagVersion(versionId, request);
        return Result.success(result);
    }

    /** 获取RAG版本详情（用于审核）
     * 
     * @param versionId 版本ID
     * @return 版本详情 */
    @GetMapping("/{versionId}")
    public Result<RagVersionDTO> getRagVersionDetail(@PathVariable String versionId) {
        RagVersionDTO result = ragPublishAppService.getRagVersionDetail(versionId, null);
        return Result.success(result);
    }

    /** 下架RAG版本
     * 
     * @param versionId 版本ID
     * @return 下架后的版本信息 */
    @PostMapping("/{versionId}/remove")
    public Result<RagVersionDTO> removeRagVersion(@PathVariable String versionId) {
        RagVersionDTO result = ragPublishAppService.removeRagVersion(versionId);
        return Result.success(result);
    }

    /** 获取RAG统计数据
     * 
     * @return 统计数据 */
    @GetMapping("/statistics")
    public Result<RagStatisticsDTO> getRagStatistics() {
        RagStatisticsDTO result = ragPublishAppService.getRagStatistics();
        return Result.success(result);
    }

    /** 获取所有RAG版本列表（管理员用）
     * 
     * @param request 查询请求
     * @return 版本列表 */
    @GetMapping("/versions")
    public Result<Page<RagVersionDTO>> getAllRagVersions(QueryRagVersionRequest request) {
        Page<RagVersionDTO> result = ragPublishAppService.getAllRagVersions(request);
        return Result.success(result);
    }

    /** 批量审核RAG版本
     * 
     * @param request 批量审核请求
     * @return 操作结果 */
    @PostMapping("/batch-review")
    public Result<String> batchReviewRagVersions(@RequestBody @Validated BatchReviewRequest request) {
        String result = ragPublishAppService.batchReviewRagVersions(request);
        return Result.success(result);
    }

    /** 获取RAG内容预览
     * 
     * @param versionId 版本ID
     * @return 内容预览 */
    @GetMapping("/{versionId}/preview")
    public Result<RagContentPreviewDTO> getRagContentPreview(@PathVariable String versionId) {
        RagContentPreviewDTO result = ragPublishAppService.getRagContentPreview(versionId);
        return Result.success(result);
    }
}