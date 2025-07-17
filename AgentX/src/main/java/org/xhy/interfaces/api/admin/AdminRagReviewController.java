package org.xhy.interfaces.api.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.rag.RagPublishAppService;
import org.xhy.application.rag.dto.RagVersionDTO;
import org.xhy.application.rag.request.ReviewRagVersionRequest;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

/** 管理员RAG审核控制器
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@RestController
@RequestMapping("/api/admin/rag/review")
public class AdminRagReviewController {

    private final RagPublishAppService ragPublishAppService;

    public AdminRagReviewController(RagPublishAppService ragPublishAppService) {
        this.ragPublishAppService = ragPublishAppService;
    }

    /** 获取待审核的RAG版本列表
     * 
     * @param page 页码
     * @param pageSize 每页大小
     * @return 待审核版本列表
     */
    @GetMapping("/pending")
    public Result<Page<RagVersionDTO>> getPendingReviewVersions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer pageSize) {
        Page<RagVersionDTO> result = ragPublishAppService.getPendingReviewVersions(page, pageSize);
        return Result.success(result);
    }

    /** 审核RAG版本
     * 
     * @param versionId 版本ID
     * @param request 审核请求
     * @return 审核后的版本信息
     */
    @PostMapping("/{versionId}")
    public Result<RagVersionDTO> reviewRagVersion(
            @PathVariable String versionId,
            @RequestBody @Validated ReviewRagVersionRequest request) {
        RagVersionDTO result = ragPublishAppService.reviewRagVersion(versionId, request);
        return Result.success(result);
    }

    /** 获取RAG版本详情（用于审核）
     * 
     * @param versionId 版本ID
     * @return 版本详情
     */
    @GetMapping("/{versionId}")
    public Result<RagVersionDTO> getRagVersionDetail(@PathVariable String versionId) {
        RagVersionDTO result = ragPublishAppService.getRagVersionDetail(versionId, null);
        return Result.success(result);
    }

    /** 下架RAG版本
     * 
     * @param versionId 版本ID
     * @return 下架后的版本信息
     */
    @PostMapping("/{versionId}/remove")
    public Result<RagVersionDTO> removeRagVersion(@PathVariable String versionId) {
        RagVersionDTO result = ragPublishAppService.removeRagVersion(versionId);
        return Result.success(result);
    }
}