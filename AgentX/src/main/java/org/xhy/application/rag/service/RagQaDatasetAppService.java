package org.xhy.application.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.rag.assembler.DocumentUnitAssembler;
import org.xhy.application.rag.assembler.FileDetailAssembler;
import org.xhy.application.rag.assembler.RagQaDatasetAssembler;
import org.xhy.application.rag.dto.*;
import org.xhy.domain.rag.constant.FileInitializeStatus;
import org.xhy.domain.rag.constant.EmbeddingStatus;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.message.RagDocSyncStorageMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.service.EmbeddingDomainService;
import org.xhy.domain.rag.service.FileDetailDomainService;
import org.xhy.domain.rag.service.RagQaDatasetDomainService;
import org.xhy.infrastructure.mq.enums.EventType;
import org.xhy.infrastructure.mq.events.RagDocSyncOcrEvent;
import org.xhy.infrastructure.mq.events.RagDocSyncStorageEvent;

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
    private final DocumentUnitRepository documentUnitRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final EmbeddingDomainService embeddingDomainService;

    public RagQaDatasetAppService(RagQaDatasetDomainService ragQaDatasetDomainService,
                                  FileDetailDomainService fileDetailDomainService,
                                  DocumentUnitRepository documentUnitRepository,
                                  ApplicationEventPublisher applicationEventPublisher,
                                  EmbeddingDomainService embeddingDomainService) {
        this.ragQaDatasetDomainService = ragQaDatasetDomainService;
        this.fileDetailDomainService = fileDetailDomainService;
        this.documentUnitRepository = documentUnitRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.embeddingDomainService = embeddingDomainService;
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

    /**
     * 启动文件预处理
     * @param request 预处理请求
     * @param userId 用户ID
     */
    @Transactional
    public void processFile(ProcessFileRequest request, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(request.getDatasetId(), userId);
        
        // 验证文件存在性和权限
        FileDetailEntity fileEntity = fileDetailDomainService.getFileById(request.getFileId(), userId);
        
        if (request.getProcessType() == 1) {
            // OCR预处理
            fileDetailDomainService.updateFileInitializeStatus(request.getFileId(), FileInitializeStatus.INITIALIZING);
            fileDetailDomainService.updateFileProgress(request.getFileId(), 0, 0.0);
            
            // 发送OCR处理MQ消息
            RagDocSyncOcrMessage ocrMessage = new RagDocSyncOcrMessage();
            ocrMessage.setFileId(request.getFileId());
            ocrMessage.setPageSize(fileEntity.getFilePageSize());
            
            RagDocSyncOcrEvent<RagDocSyncOcrMessage> ocrEvent = new RagDocSyncOcrEvent<>(ocrMessage, EventType.DOC_REFRESH_ORG);
            ocrEvent.setDescription("文件OCR预处理任务");
            applicationEventPublisher.publishEvent(ocrEvent);
            
        } else if (request.getProcessType() == 2) {
            // 向量化处理
            if (!fileEntity.getIsInitialize().equals(FileInitializeStatus.INITIALIZED)) {
                throw new IllegalStateException("文件需要先完成初始化才能进行向量化");
            }

            fileDetailDomainService.updateFileEmbeddingStatus(request.getFileId(), EmbeddingStatus.INITIALIZING);
            fileDetailDomainService.updateFileProgress(request.getFileId(), 0, 0.0);

            List<DocumentUnitEntity> documentUnits = documentUnitRepository.selectList(Wrappers.lambdaQuery(
                    DocumentUnitEntity.class).eq(DocumentUnitEntity::getFileId, request.getFileId())
                    .eq(DocumentUnitEntity::getIsOcr, true)
                    .eq(DocumentUnitEntity::getIsVector, false));
            
            if (documentUnits.isEmpty()) {
                throw new IllegalStateException("文件没有找到可用于向量化的语料数据");
            }
            
            // 为每个DocumentUnit发送单独的向量化MQ消息
            for (DocumentUnitEntity documentUnit : documentUnits) {
                RagDocSyncStorageMessage storageMessage = new RagDocSyncStorageMessage();
                storageMessage.setId(documentUnit.getId());
                storageMessage.setFileId(request.getFileId());
                storageMessage.setFileName(fileEntity.getOriginalFilename());
                storageMessage.setPage(documentUnit.getPage());
                storageMessage.setContent(documentUnit.getContent());
                storageMessage.setVector(true);
                storageMessage.setDatasetId(request.getDatasetId());  // 设置数据集ID
                
                RagDocSyncStorageEvent<RagDocSyncStorageMessage> storageEvent = new RagDocSyncStorageEvent<>(storageMessage, EventType.DOC_SYNC_RAG);
                storageEvent.setDescription("文件向量化处理任务 - 页面 " + documentUnit.getPage());
                applicationEventPublisher.publishEvent(storageEvent);
            }
            
        } else {
            throw new IllegalArgumentException("不支持的处理类型: " + request.getProcessType());
        }
    }

    /**
     * 获取文件处理进度
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 处理进度
     */
    public FileProcessProgressDTO getFileProgress(String fileId, String userId) {
        FileDetailEntity fileEntity = fileDetailDomainService.getFileById(fileId, userId);
        return buildFileProgressDTO(fileEntity);
    }

    /**
     * 获取数据集文件处理进度列表
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 处理进度列表
     */
    public List<FileProcessProgressDTO> getDatasetFilesProgress(String datasetId, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);
        
        List<FileDetailEntity> entities = fileDetailDomainService.listAllFilesByDataset(datasetId, userId);
        return entities.stream()
                .map(this::buildFileProgressDTO)
                .toList();
    }

    /**
     * 构建文件处理进度DTO
     * @param entity 文件实体
     * @return 处理进度DTO
     */
    private FileProcessProgressDTO buildFileProgressDTO(FileDetailEntity entity) {
        FileProcessProgressDTO dto = new FileProcessProgressDTO();
        dto.setFileId(entity.getId());
        dto.setFilename(entity.getOriginalFilename());
        dto.setIsInitialize(entity.getIsInitialize());
        dto.setIsEmbedding(entity.getIsEmbedding());
        dto.setCurrentPageNumber(entity.getCurrentPageNumber() != null ? entity.getCurrentPageNumber() : 0);
        dto.setFilePageSize(entity.getFilePageSize() != null ? entity.getFilePageSize() : 0);
        dto.setProcessProgress(entity.getProcessProgress() != null ? entity.getProcessProgress() : 0.0);
        dto.setStatusDescription(getStatusDescription(entity));
        return dto;
    }

    /**
     * 获取状态描述
     * @param entity 文件实体
     * @return 状态描述
     */
    private String getStatusDescription(FileDetailEntity entity) {
        Integer initStatus = entity.getIsInitialize();
        Integer embeddingStatus = entity.getIsEmbedding();
        
        if (initStatus == null || initStatus == 0) {
            return "待初始化";
        } else if (initStatus == 1) {
            return "初始化中";
        } else if (initStatus == 3) {
            return "初始化失败";
        } else if (initStatus == 2) {
            if (embeddingStatus == null || embeddingStatus == 0) {
                return "初始化完成，待向量化";
            } else if (embeddingStatus == 1) {
                return "向量化中";
            } else if (embeddingStatus == 3) {
                return "向量化失败";
            } else if (embeddingStatus == 2) {
                return "处理完成";
            }
        }
        return "未知状态";
    }

    /**
     * RAG搜索文档
     * @param request 搜索请求
     * @param userId 用户ID
     * @return 搜索结果
     */
    public List<DocumentUnitDTO> ragSearch(RagSearchRequest request, String userId) {
        // 验证数据集权限 - 检查用户是否有这些数据集的访问权限
        for (String datasetId : request.getDatasetIds()) {
            ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);
        }
        
        // 调用领域服务进行RAG搜索
        List<DocumentUnitEntity> entities = embeddingDomainService.ragDoc(request.getDatasetIds(), request.getQuestion(), request.getMaxResults());
        
        // 转换为DTO并返回
        return DocumentUnitAssembler.toDTOs(entities);
    }
}