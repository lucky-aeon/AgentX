package org.xhy.interfaces.api.portal.rag;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.rag.dto.*;
import org.xhy.application.rag.service.FileOperationAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

/** 文件操作控制器
 * 
 * @author shilong.zang
 * @date 2024-12-09 */
@RestController
@RequestMapping("/rag/files")
public class FileOperationController {

    private final FileOperationAppService fileOperationAppService;

    public FileOperationController(FileOperationAppService fileOperationAppService) {
        this.fileOperationAppService = fileOperationAppService;
    }

    /** 根据文件ID获取文件详细信息（包含文件路径）
     * 
     * @param fileId 文件ID
     * @return 文件详细信息 */
    @GetMapping("/{fileId}/info")
    public Result<FileDetailInfoDTO> getFileDetailInfo(@PathVariable String fileId) {
        String userId = UserContext.getCurrentUserId();
        FileDetailInfoDTO fileInfo = fileOperationAppService.getFileDetailInfo(fileId, userId);
        return Result.success(fileInfo);
    }

    /** 批量删除文件
     * 
     * @param request 批量删除请求
     * @return 操作结果 */
    @PostMapping("/batch-delete")
    public Result<Void> batchDeleteFiles(@RequestBody @Validated BatchDeleteFilesRequest request) {
        String userId = UserContext.getCurrentUserId();
        fileOperationAppService.batchDeleteFiles(request, userId);
        return Result.success();
    }

    /** 分页查询文件的语料
     * 
     * @param request 查询请求
     * @return 语料分页列表 */
    @PostMapping("/document-units/list")
    public Result<Page<DocumentUnitDTO>> listDocumentUnits(@RequestBody @Validated QueryDocumentUnitsRequest request) {
        String userId = UserContext.getCurrentUserId();
        Page<DocumentUnitDTO> result = fileOperationAppService.listDocumentUnits(request, userId);
        return Result.success(result);
    }

    /** 更新语料内容
     * 
     * @param request 更新请求
     * @return 更新后的语料信息 */
    @PutMapping("/document-units")
    public Result<DocumentUnitDTO> updateDocumentUnit(@RequestBody @Validated UpdateDocumentUnitRequest request) {
        String userId = UserContext.getCurrentUserId();
        DocumentUnitDTO documentUnit = fileOperationAppService.updateDocumentUnit(request, userId);
        return Result.success(documentUnit);
    }

    /** 删除语料
     * 
     * @param documentUnitId 语料ID
     * @return 操作结果 */
    @DeleteMapping("/document-units/{documentUnitId}")
    public Result<Void> deleteDocumentUnit(@PathVariable String documentUnitId) {
        String userId = UserContext.getCurrentUserId();
        fileOperationAppService.deleteDocumentUnit(documentUnitId, userId);
        return Result.success();
    }

    /** 根据语料ID获取单个语料详情
     * 
     * @param documentUnitId 语料ID
     * @return 语料详情 */
    @GetMapping("/document-units/{documentUnitId}")
    public Result<DocumentUnitDTO> getDocumentUnit(@PathVariable String documentUnitId) {
        String userId = UserContext.getCurrentUserId();
        DocumentUnitDTO documentUnit = fileOperationAppService.getDocumentUnit(documentUnitId, userId);
        return Result.success(documentUnit);
    }
}