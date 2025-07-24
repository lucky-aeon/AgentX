package org.xhy.interfaces.api.portal.rag;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.rag.RagMarketAppService;
import org.xhy.application.rag.dto.DocumentUnitDTO;
import org.xhy.application.rag.dto.FileDetailDTO;
import org.xhy.application.rag.dto.RagMarketDTO;
import org.xhy.application.rag.dto.UserRagDTO;
import org.xhy.application.rag.request.InstallRagRequest;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;
import org.xhy.interfaces.dto.rag.request.QueryRagMarketRequest;
import org.xhy.interfaces.dto.rag.request.QueryUserInstalledRagRequest;

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
     * @param request 查询请求
     * @return RAG市场列表 */
    @GetMapping
    public Result<Page<RagMarketDTO>> getMarketRagVersions(QueryRagMarketRequest request) {
        String userId = UserContext.getCurrentUserId();
        Page<RagMarketDTO> result = ragMarketAppService.getMarketRagVersions(request, userId);
        return Result.success(result);
    }

    /** 安装RAG版本
     * 
     * @param request 安装请求
     * @return 安装后的RAG信息 */
    @PostMapping("/install")
    public Result<UserRagDTO> installRagVersion(@RequestBody @Validated InstallRagRequest request) {
        String userId = UserContext.getCurrentUserId();
        UserRagDTO result = ragMarketAppService.installRagVersion(request, userId);
        return Result.success(result);
    }

    /** 卸载RAG版本
     * 
     * @param ragVersionId RAG版本ID
     * @return 操作结果 */
    @DeleteMapping("/uninstall/{ragVersionId}")
    public Result<Void> uninstallRagVersion(@PathVariable String ragVersionId) {
        String userId = UserContext.getCurrentUserId();
        ragMarketAppService.uninstallRagVersion(ragVersionId, userId);
        return Result.success();
    }

    /** 获取用户安装的RAG列表
     * 
     * @param request 查询请求
     * @return 用户安装的RAG列表 */
    @GetMapping("/installed")
    public Result<Page<UserRagDTO>> getUserInstalledRags(QueryUserInstalledRagRequest request) {
        String userId = UserContext.getCurrentUserId();
        Page<UserRagDTO> result = ragMarketAppService.getUserInstalledRags(userId, request);
        return Result.success(result);
    }

    /** 获取用户安装的所有RAG（用于对话中选择）
     * 
     * @return 用户安装的RAG列表 */
    @GetMapping("/installed/all")
    public Result<List<UserRagDTO>> getUserAllInstalledRags() {
        String userId = UserContext.getCurrentUserId();
        List<UserRagDTO> result = ragMarketAppService.getUserAllInstalledRags(userId);
        return Result.success(result);
    }

    /** 获取用户安装的RAG详情
     * 
     * @param ragVersionId RAG版本ID
     * @return 安装的RAG详情 */
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
     * @return 是否有权限 */
    @GetMapping("/permission/check")
    public Result<Boolean> canUseRag(@RequestParam(required = false) String ragId,
            @RequestParam(required = false) String ragVersionId) {
        String userId = UserContext.getCurrentUserId();
        boolean result = ragMarketAppService.canUseRag(userId, ragId, ragVersionId);
        return Result.success(result);
    }

    /** 切换已安装RAG的版本
     * 
     * @param userRagId 用户RAG安装记录ID
     * @param targetVersionId 目标版本ID
     * @return 切换后的RAG信息 */
    @PutMapping("/installed/{userRagId}/switch-version")
    public Result<UserRagDTO> switchRagVersion(@PathVariable String userRagId, @RequestParam String targetVersionId) {
        String userId = UserContext.getCurrentUserId();
        UserRagDTO result = ragMarketAppService.switchRagVersion(userRagId, targetVersionId, userId);
        return Result.success(result);
    }

    /** 获取已安装RAG的所有版本列表
     * 
     * @param userRagId 用户RAG安装记录ID
     * @return 同一RAG的所有已安装版本列表 */
    @GetMapping("/installed/{userRagId}/versions")
    public Result<List<UserRagDTO>> getInstalledRagVersions(@PathVariable String userRagId) {
        String userId = UserContext.getCurrentUserId();
        List<UserRagDTO> result = ragMarketAppService.getInstalledRagVersions(userRagId, userId);
        return Result.success(result);
    }

    /** 获取已安装RAG的文件列表
     * 
     * @param userRagId 用户RAG安装记录ID
     * @return 文件列表 */
    @GetMapping("/installed/{userRagId}/files")
    public Result<List<FileDetailDTO>> getInstalledRagFiles(@PathVariable String userRagId) {
        String userId = UserContext.getCurrentUserId();
        List<FileDetailDTO> result = ragMarketAppService.getInstalledRagFilesDTO(userRagId, userId);
        return Result.success(result);
    }

    /** 获取已安装RAG的所有文档单元
     * 
     * @param userRagId 用户RAG安装记录ID
     * @return 文档单元列表 */
    @GetMapping("/installed/{userRagId}/documents")
    public Result<List<DocumentUnitDTO>> getInstalledRagDocuments(@PathVariable String userRagId) {
        String userId = UserContext.getCurrentUserId();
        List<DocumentUnitDTO> result = ragMarketAppService.getInstalledRagDocumentsDTO(userRagId, userId);
        return Result.success(result);
    }

    /** 获取已安装RAG特定文件的信息
     * 
     * @param userRagId 用户RAG安装记录ID
     * @param fileId 文件ID
     * @return 文件详细信息 */
    @GetMapping("/installed/{userRagId}/files/{fileId}/info")
    public Result<FileDetailDTO> getInstalledRagFileInfo(@PathVariable String userRagId, @PathVariable String fileId) {
        String userId = UserContext.getCurrentUserId();
        FileDetailDTO result = ragMarketAppService.getInstalledRagFileInfoDTO(userRagId, fileId, userId);
        return Result.success(result);
    }

    /** 获取已安装RAG特定文件的文档单元
     * 
     * @param userRagId 用户RAG安装记录ID
     * @param fileId 文件ID
     * @return 文档单元列表 */
    @GetMapping("/installed/{userRagId}/files/{fileId}/documents")
    public Result<List<DocumentUnitDTO>> getInstalledRagFileDocuments(@PathVariable String userRagId,
            @PathVariable String fileId) {
        String userId = UserContext.getCurrentUserId();
        List<DocumentUnitDTO> result = ragMarketAppService.getInstalledRagFileDocumentsDTO(userRagId, fileId, userId);
        return Result.success(result);
    }

    /** 获取市场上RAG版本的文件列表
     * 
     * @param ragVersionId RAG版本ID
     * @return 文件列表 */
    @GetMapping("/{ragVersionId}/files")
    public Result<List<FileDetailDTO>> getMarketRagFiles(@PathVariable String ragVersionId) {
        List<FileDetailDTO> result = ragMarketAppService.getMarketRagFilesDTO(ragVersionId);
        return Result.success(result);
    }

    /** 获取市场上RAG版本特定文件的文档单元
     * 
     * @param ragVersionId RAG版本ID
     * @param fileId 文件ID
     * @return 文档单元列表 */
    @GetMapping("/{ragVersionId}/files/{fileId}/documents")
    public Result<List<DocumentUnitDTO>> getMarketRagFileDocuments(@PathVariable String ragVersionId,
            @PathVariable String fileId) {
        List<DocumentUnitDTO> result = ragMarketAppService.getMarketRagFileDocumentsDTO(ragVersionId, fileId);
        return Result.success(result);
    }
}