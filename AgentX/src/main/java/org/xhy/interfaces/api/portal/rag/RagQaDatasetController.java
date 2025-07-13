package org.xhy.interfaces.api.portal.rag;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.application.rag.dto.*;
import org.xhy.application.rag.service.RagQaDatasetAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/** RAG数据集控制器
 * @author shilong.zang
 * @date 2024-12-09 */
@RestController
@RequestMapping("/rag/datasets")
public class RagQaDatasetController {

    private final RagQaDatasetAppService ragQaDatasetAppService;

    public RagQaDatasetController(RagQaDatasetAppService ragQaDatasetAppService) {
        this.ragQaDatasetAppService = ragQaDatasetAppService;
    }

    /** 创建数据集
     * @param request 创建请求
     * @return 数据集信息 */
    @PostMapping
    public Result<RagQaDatasetDTO> createDataset(@RequestBody @Validated CreateDatasetRequest request) {
        String userId = UserContext.getCurrentUserId();
        RagQaDatasetDTO dataset = ragQaDatasetAppService.createDataset(request, userId);
        return Result.success(dataset);
    }

    /** 更新数据集
     * @param datasetId 数据集ID
     * @param request 更新请求
     * @return 数据集信息 */
    @PutMapping("/{datasetId}")
    public Result<RagQaDatasetDTO> updateDataset(@PathVariable String datasetId,
            @RequestBody @Validated UpdateDatasetRequest request) {
        String userId = UserContext.getCurrentUserId();
        RagQaDatasetDTO dataset = ragQaDatasetAppService.updateDataset(datasetId, request, userId);
        return Result.success(dataset);
    }

    /** 删除数据集
     * @param datasetId 数据集ID
     * @return 操作结果 */
    @DeleteMapping("/{datasetId}")
    public Result<Void> deleteDataset(@PathVariable String datasetId) {
        String userId = UserContext.getCurrentUserId();
        ragQaDatasetAppService.deleteDataset(datasetId, userId);
        return Result.success();
    }

    /** 获取数据集详情
     * @param datasetId 数据集ID
     * @return 数据集信息 */
    @GetMapping("/{datasetId}")
    public Result<RagQaDatasetDTO> getDataset(@PathVariable String datasetId) {
        String userId = UserContext.getCurrentUserId();
        RagQaDatasetDTO dataset = ragQaDatasetAppService.getDataset(datasetId, userId);
        return Result.success(dataset);
    }

    /** 分页查询数据集
     * @param request 查询请求
     * @return 分页结果 */
    @GetMapping
    public Result<Page<RagQaDatasetDTO>> listDatasets(QueryDatasetRequest request) {
        String userId = UserContext.getCurrentUserId();
        Page<RagQaDatasetDTO> result = ragQaDatasetAppService.listDatasets(request, userId);
        return Result.success(result);
    }

    /** 获取所有数据集
     * @return 数据集列表 */
    @GetMapping("/all")
    public Result<List<RagQaDatasetDTO>> listAllDatasets() {
        String userId = UserContext.getCurrentUserId();
        List<RagQaDatasetDTO> datasets = ragQaDatasetAppService.listAllDatasets(userId);
        return Result.success(datasets);
    }

    /** 上传文件到数据集
     * @param request 上传请求
     * @return 文件信息 */
    @PostMapping("/files")
    public Result<FileDetailDTO> uploadFile(@Validated UploadFileRequest request) {
        String userId = UserContext.getCurrentUserId();
        FileDetailDTO file = ragQaDatasetAppService.uploadFile(request, userId);
        return Result.success(file);
    }

    /** 删除数据集文件
     * @param datasetId 数据集ID
     * @param fileId 文件ID
     * @return 操作结果 */
    @DeleteMapping("/{datasetId}/files/{fileId}")
    public Result<Void> deleteFile(@PathVariable String datasetId, @PathVariable String fileId) {
        String userId = UserContext.getCurrentUserId();
        ragQaDatasetAppService.deleteFile(datasetId, fileId, userId);
        return Result.success();
    }

    /** 分页查询数据集文件
     * @param datasetId 数据集ID
     * @param request 查询请求
     * @return 分页结果 */
    @GetMapping("/{datasetId}/files")
    public Result<Page<FileDetailDTO>> listDatasetFiles(@PathVariable String datasetId,
            QueryDatasetFileRequest request) {
        String userId = UserContext.getCurrentUserId();
        Page<FileDetailDTO> result = ragQaDatasetAppService.listDatasetFiles(datasetId, request, userId);
        return Result.success(result);
    }

    /** 获取数据集所有文件
     * @param datasetId 数据集ID
     * @return 文件列表 */
    @GetMapping("/{datasetId}/files/all")
    public Result<List<FileDetailDTO>> listAllDatasetFiles(@PathVariable String datasetId) {
        String userId = UserContext.getCurrentUserId();
        List<FileDetailDTO> files = ragQaDatasetAppService.listAllDatasetFiles(datasetId, userId);
        return Result.success(files);
    }

    /** 启动文件预处理
     * @param request 预处理请求
     * @return 操作结果 */
    @PostMapping("/files/process")
    public Result<Void> processFile(@RequestBody @Validated ProcessFileRequest request) {
        String userId = UserContext.getCurrentUserId();
        ragQaDatasetAppService.processFile(request, userId);
        return Result.success();
    }

    /** 获取文件处理进度
     * @param fileId 文件ID
     * @return 处理进度 */
    @GetMapping("/files/{fileId}/progress")
    public Result<FileProcessProgressDTO> getFileProgress(@PathVariable String fileId) {
        String userId = UserContext.getCurrentUserId();
        FileProcessProgressDTO progress = ragQaDatasetAppService.getFileProgress(fileId, userId);
        return Result.success(progress);
    }

    /** 获取数据集文件处理进度列表
     * @param datasetId 数据集ID
     * @return 处理进度列表 */
    @GetMapping("/{datasetId}/files/progress")
    public Result<List<FileProcessProgressDTO>> getDatasetFilesProgress(@PathVariable String datasetId) {
        String userId = UserContext.getCurrentUserId();
        List<FileProcessProgressDTO> progressList = ragQaDatasetAppService.getDatasetFilesProgress(datasetId, userId);
        return Result.success(progressList);
    }
}