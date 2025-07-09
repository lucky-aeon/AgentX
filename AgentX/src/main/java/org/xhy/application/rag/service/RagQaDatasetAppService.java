package org.xhy.application.rag.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.rag.assembler.FileDetailAssembler;
import org.xhy.application.rag.assembler.RagQaDatasetAssembler;
import org.xhy.application.rag.dto.*;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.service.FileDetailDomainService;
import org.xhy.domain.rag.service.RagQaDatasetDomainService;

import java.util.List;

/**
 * RAG数据集应用服务
 * @author shilong.zang
 * @date 2024-12-09
 */
@Service
public class RagQaDatasetAppService {

    private final RagQaDatasetDomainService ragQaDatasetDomainService;
    private final FileDetailDomainService fileDetailDomainService;

    public RagQaDatasetAppService(RagQaDatasetDomainService ragQaDatasetDomainService,
                                  FileDetailDomainService fileDetailDomainService) {
        this.ragQaDatasetDomainService = ragQaDatasetDomainService;
        this.fileDetailDomainService = fileDetailDomainService;
    }

    /**
     * 创建数据集
     * @param request 创建请求
     * @param userId 用户ID
     * @return 数据集DTO
     */
    @Transactional
    public RagQaDatasetDTO createDataset(CreateDatasetRequest request, String userId) {
        RagQaDatasetEntity entity = RagQaDatasetAssembler.toEntity(request, userId);
        RagQaDatasetEntity createdEntity = ragQaDatasetDomainService.createDataset(entity);
        return RagQaDatasetAssembler.toDTO(createdEntity, 0L);
    }

    /**
     * 更新数据集
     * @param datasetId 数据集ID
     * @param request 更新请求
     * @param userId 用户ID
     * @return 数据集DTO
     */
    @Transactional
    public RagQaDatasetDTO updateDataset(String datasetId, UpdateDatasetRequest request, String userId) {
        RagQaDatasetEntity entity = RagQaDatasetAssembler.toEntity(request, datasetId, userId);
        ragQaDatasetDomainService.updateDataset(entity);
        
        // 获取更新后的实体
        RagQaDatasetEntity updatedEntity = ragQaDatasetDomainService.getDataset(datasetId, userId);
        Long fileCount = fileDetailDomainService.countFilesByDataset(datasetId, userId);
        return RagQaDatasetAssembler.toDTO(updatedEntity, fileCount);
    }

    /**
     * 删除数据集
     * @param datasetId 数据集ID
     * @param userId 用户ID
     */
    @Transactional
    public void deleteDataset(String datasetId, String userId) {
        // 先删除数据集下的所有文件
        fileDetailDomainService.deleteAllFilesByDataset(datasetId, userId);
        
        // 再删除数据集
        ragQaDatasetDomainService.deleteDataset(datasetId, userId);
    }

    /**
     * 获取数据集详情
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 数据集DTO
     */
    public RagQaDatasetDTO getDataset(String datasetId, String userId) {
        RagQaDatasetEntity entity = ragQaDatasetDomainService.getDataset(datasetId, userId);
        Long fileCount = fileDetailDomainService.countFilesByDataset(datasetId, userId);
        return RagQaDatasetAssembler.toDTO(entity, fileCount);
    }

    /**
     * 分页查询数据集
     * @param request 查询请求
     * @param userId 用户ID
     * @return 分页结果
     */
    public Page<RagQaDatasetDTO> listDatasets(QueryDatasetRequest request, String userId) {
        IPage<RagQaDatasetEntity> entityPage = ragQaDatasetDomainService.listDatasets(
                userId, request.getPage(), request.getPageSize(), request.getKeyword()
        );
        
        Page<RagQaDatasetDTO> dtoPage = new Page<>(
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal()
        );
        
        // 转换为DTO并添加文件数量
        List<RagQaDatasetDTO> dtoList = entityPage.getRecords().stream()
                .map(entity -> {
                    Long fileCount = fileDetailDomainService.countFilesByDataset(entity.getId(), userId);
                    return RagQaDatasetAssembler.toDTO(entity, fileCount);
                })
                .toList();
        
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    /**
     * 获取所有数据集
     * @param userId 用户ID
     * @return 数据集列表
     */
    public List<RagQaDatasetDTO> listAllDatasets(String userId) {
        List<RagQaDatasetEntity> entities = ragQaDatasetDomainService.listAllDatasets(userId);
        return entities.stream()
                .map(entity -> {
                    Long fileCount = fileDetailDomainService.countFilesByDataset(entity.getId(), userId);
                    return RagQaDatasetAssembler.toDTO(entity, fileCount);
                })
                .toList();
    }

    /**
     * 上传文件到数据集
     * @param request 上传请求
     * @param userId 用户ID
     * @return 文件DTO
     */
    @Transactional
    public FileDetailDTO uploadFile(UploadFileRequest request, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(request.getDatasetId(), userId);
        
        // 上传文件
        FileDetailEntity entity = FileDetailAssembler.toEntity(request, userId);
        FileDetailEntity uploadedEntity = fileDetailDomainService.uploadFileToDataset(entity);
        return FileDetailAssembler.toDTO(uploadedEntity);
    }

    /**
     * 删除数据集文件
     * @param datasetId 数据集ID
     * @param fileId 文件ID
     * @param userId 用户ID
     */
    @Transactional
    public void deleteFile(String datasetId, String fileId, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);
        
        // 删除文件
        fileDetailDomainService.deleteFile(fileId, userId);
    }

    /**
     * 分页查询数据集文件
     * @param datasetId 数据集ID
     * @param request 查询请求
     * @param userId 用户ID
     * @return 分页结果
     */
    public Page<FileDetailDTO> listDatasetFiles(String datasetId, QueryDatasetFileRequest request, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);
        
        IPage<FileDetailEntity> entityPage = fileDetailDomainService.listFilesByDataset(
                datasetId, userId, request.getPage(), request.getPageSize(), request.getKeyword()
        );
        
        Page<FileDetailDTO> dtoPage = new Page<>(
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal()
        );
        
        List<FileDetailDTO> dtoList = FileDetailAssembler.toDTOs(entityPage.getRecords());
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    /**
     * 获取数据集所有文件
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 文件列表
     */
    public List<FileDetailDTO> listAllDatasetFiles(String datasetId, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);
        
        List<FileDetailEntity> entities = fileDetailDomainService.listAllFilesByDataset(datasetId, userId);
        return FileDetailAssembler.toDTOs(entities);
    }
}