package org.xhy.interfaces.api.portal.rag;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.rag.RagPublishAppService;
import org.xhy.application.rag.dto.RagVersionDTO;
import org.xhy.application.rag.request.PublishRagRequest;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/** RAG发布控制器
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@RestController
@RequestMapping("/rag/publish")
public class RagPublishController {

    private final RagPublishAppService ragPublishAppService;

    public RagPublishController(RagPublishAppService ragPublishAppService) {
        this.ragPublishAppService = ragPublishAppService;
    }

    /** 发布RAG版本
     * 
     * @param request 发布请求
     * @return 发布的版本信息
     */
    @PostMapping
    public Result<RagVersionDTO> publishRagVersion(@RequestBody @Validated PublishRagRequest request) {
        String userId = UserContext.getCurrentUserId();
        RagVersionDTO result = ragPublishAppService.publishRagVersion(request, userId);
        return Result.success(result);
    }

    /** 获取用户的RAG版本列表
     * 
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @return 版本列表
     */
    @GetMapping("/versions")
    public Result<Page<RagVersionDTO>> getUserRagVersions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        String userId = UserContext.getCurrentUserId();
        Page<RagVersionDTO> result = ragPublishAppService.getUserRagVersions(userId, page, pageSize, keyword);
        return Result.success(result);
    }

    /** 获取RAG的版本历史
     * 
     * @param ragId 原始RAG数据集ID
     * @return 版本历史列表
     */
    @GetMapping("/versions/history/{ragId}")
    public Result<List<RagVersionDTO>> getRagVersionHistory(@PathVariable String ragId) {
        String userId = UserContext.getCurrentUserId();
        List<RagVersionDTO> result = ragPublishAppService.getRagVersionHistory(ragId, userId);
        return Result.success(result);
    }

    /** 获取RAG版本详情
     * 
     * @param versionId 版本ID
     * @return 版本详情
     */
    @GetMapping("/versions/{versionId}")
    public Result<RagVersionDTO> getRagVersionDetail(@PathVariable String versionId) {
        String userId = UserContext.getCurrentUserId();
        RagVersionDTO result = ragPublishAppService.getRagVersionDetail(versionId, userId);
        return Result.success(result);
    }
}