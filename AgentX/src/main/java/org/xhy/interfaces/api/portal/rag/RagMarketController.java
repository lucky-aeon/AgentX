package org.xhy.interfaces.api.portal.rag;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.rag.RagMarketAppService;
import org.xhy.application.rag.dto.RagMarketDTO;
import org.xhy.application.rag.dto.UserRagDTO;
import org.xhy.application.rag.request.InstallRagRequest;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/** RAG市场控制器
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@RestController
@RequestMapping("/rag/market")
public class RagMarketController {

    private final RagMarketAppService ragMarketAppService;

    public RagMarketController(RagMarketAppService ragMarketAppService) {
        this.ragMarketAppService = ragMarketAppService;
    }

    /** 获取市场上的RAG版本列表
     * 
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @return RAG市场列表
     */
    @GetMapping
    public Result<Page<RagMarketDTO>> getMarketRagVersions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        String userId = UserContext.getCurrentUserId();
        Page<RagMarketDTO> result = ragMarketAppService.getMarketRagVersions(page, pageSize, keyword, userId);
        return Result.success(result);
    }

    /** 安装RAG版本
     * 
     * @param request 安装请求
     * @return 安装后的RAG信息
     */
    @PostMapping("/install")
    public Result<UserRagDTO> installRagVersion(@RequestBody @Validated InstallRagRequest request) {
        String userId = UserContext.getCurrentUserId();
        UserRagDTO result = ragMarketAppService.installRagVersion(request, userId);
        return Result.success(result);
    }

    /** 卸载RAG版本
     * 
     * @param ragVersionId RAG版本ID
     * @return 操作结果
     */
    @DeleteMapping("/uninstall/{ragVersionId}")
    public Result<Void> uninstallRagVersion(@PathVariable String ragVersionId) {
        String userId = UserContext.getCurrentUserId();
        ragMarketAppService.uninstallRagVersion(ragVersionId, userId);
        return Result.success();
    }

    /** 获取用户安装的RAG列表
     * 
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @return 用户安装的RAG列表
     */
    @GetMapping("/installed")
    public Result<Page<UserRagDTO>> getUserInstalledRags(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        String userId = UserContext.getCurrentUserId();
        Page<UserRagDTO> result = ragMarketAppService.getUserInstalledRags(userId, page, pageSize, keyword);
        return Result.success(result);
    }

    /** 获取用户安装的所有RAG（用于对话中选择）
     * 
     * @return 用户安装的RAG列表
     */
    @GetMapping("/installed/all")
    public Result<List<UserRagDTO>> getUserAllInstalledRags() {
        String userId = UserContext.getCurrentUserId();
        List<UserRagDTO> result = ragMarketAppService.getUserAllInstalledRags(userId);
        return Result.success(result);
    }

    /** 更新安装的RAG状态
     * 
     * @param ragVersionId RAG版本ID
     * @param isActive 是否激活
     * @return 操作结果
     */
    @PutMapping("/installed/{ragVersionId}/status")
    public Result<Void> updateRagStatus(
            @PathVariable String ragVersionId,
            @RequestParam Boolean isActive) {
        String userId = UserContext.getCurrentUserId();
        ragMarketAppService.updateRagStatus(ragVersionId, isActive, userId);
        return Result.success();
    }

    /** 获取用户安装的RAG详情
     * 
     * @param ragVersionId RAG版本ID
     * @return 安装的RAG详情
     */
    @GetMapping("/installed/{ragVersionId}")
    public Result<UserRagDTO> getInstalledRagDetail(@PathVariable String ragVersionId) {
        String userId = UserContext.getCurrentUserId();
        UserRagDTO result = ragMarketAppService.getInstalledRagDetail(ragVersionId, userId);
        return Result.success(result);
    }

    /** 检查用户是否有权限使用RAG
     * 
     * @param ragId 原始RAG数据集ID
     * @param ragVersionId RAG版本ID
     * @return 是否有权限
     */
    @GetMapping("/permission/check")
    public Result<Boolean> canUseRag(
            @RequestParam(required = false) String ragId,
            @RequestParam(required = false) String ragVersionId) {
        String userId = UserContext.getCurrentUserId();
        boolean result = ragMarketAppService.canUseRag(userId, ragId, ragVersionId);
        return Result.success(result);
    }
}