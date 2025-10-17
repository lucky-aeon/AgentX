package org.xhy.application.rag.service.manager;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Collections;

import org.dromara.streamquery.stream.core.stream.Steam;
import org.xhy.infrastructure.mq.core.MessageEnvelope;
import org.xhy.infrastructure.mq.core.MessagePublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.application.rag.assembler.FileDetailAssembler;
import org.xhy.application.rag.assembler.FileProcessProgressAssembler;
import org.xhy.application.rag.assembler.RagQaDatasetAssembler;
import org.xhy.domain.rag.model.UserRagEntity;
import org.xhy.application.rag.dto.*;
import org.xhy.application.rag.request.PublishRagRequest;
import org.xhy.domain.rag.service.management.RagDataAccessDomainService;
import org.xhy.domain.rag.service.management.RagVersionDomainService;
import org.xhy.domain.rag.service.management.UserRagDomainService;
import org.xhy.domain.rag.service.management.UserRagFileDomainService;
import org.xhy.infrastructure.rag.factory.EmbeddingModelFactory;
import org.xhy.domain.rag.constant.FileProcessingStatusEnum;
import org.xhy.domain.rag.message.RagDocMessage;
import org.xhy.domain.rag.message.RagDocSyncStorageMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.model.RagVersionEntity;
import org.xhy.domain.rag.service.*;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.mq.enums.EventType;
import org.xhy.infrastructure.mq.events.RagDocSyncOcrEvent;
import org.xhy.infrastructure.mq.events.RagDocSyncStorageEvent;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.user.service.UserSettingsDomainService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xhy.infrastructure.rag.service.UserModelConfigResolver;

import java.util.List;
import java.util.ArrayList;

/** RAG数据集应用服务
 * @author shilong.zang
 * @date 2024-12-09 */
@Service
public class RagQaDatasetAppService {

    private static final Logger log = LoggerFactory.getLogger(RagQaDatasetAppService.class);

    private final RagQaDatasetDomainService ragQaDatasetDomainService;
    private final FileDetailDomainService fileDetailDomainService;
    private final DocumentUnitDomainService documentUnitDomainService;
    private final MessagePublisher messagePublisher;
    private final EmbeddingDomainService embeddingDomainService;

    // 添加RAG发布和市场服务依赖
    private final RagPublishAppService ragPublishAppService;
    private final RagVersionDomainService ragVersionDomainService;
    private final UserRagDomainService userRagDomainService;
    private final RagDataAccessDomainService ragDataAccessService;

    private final UserModelConfigResolver userModelConfigResolver;

    public RagQaDatasetAppService(RagQaDatasetDomainService ragQaDatasetDomainService,
            FileDetailDomainService fileDetailDomainService, DocumentUnitDomainService documentUnitDomainService,
            MessagePublisher messagePublisher, EmbeddingDomainService embeddingDomainService,
            RagPublishAppService ragPublishAppService, RagVersionDomainService ragVersionDomainService,
            UserRagDomainService userRagDomainService, RagDataAccessDomainService ragDataAccessService,
            UserModelConfigResolver userModelConfigResolver) {
        this.ragQaDatasetDomainService = ragQaDatasetDomainService;
        this.fileDetailDomainService = fileDetailDomainService;
        this.documentUnitDomainService = documentUnitDomainService;
        this.messagePublisher = messagePublisher;
        this.embeddingDomainService = embeddingDomainService;
        this.ragPublishAppService = ragPublishAppService;
        this.ragVersionDomainService = ragVersionDomainService;
        this.userRagDomainService = userRagDomainService;
        this.ragDataAccessService = ragDataAccessService;
        this.userModelConfigResolver = userModelConfigResolver;
    }

    /** 创建数据集
     * @param request 创建请求
     * @param userId 用户ID
     * @return 数据集DTO */
    @Transactional(transactionManager = "transactionManager")
    public RagQaDatasetDTO createDataset(CreateDatasetRequest request, String userId) {
        RagQaDatasetEntity entity = RagQaDatasetAssembler.toEntity(request, userId);
        RagQaDatasetEntity createdEntity = ragQaDatasetDomainService.createDataset(entity);

        // 自动创建0.0.1版本并安装给用户
        try {
            createAndInstallInitialVersion(createdEntity.getId(), userId);
        } catch (Exception e) {
            log.warn("Failed to create initial version for dataset {}: {}", createdEntity.getId(), e.getMessage());
            // 不影响数据集创建，只记录警告
        }

        return RagQaDatasetAssembler.toDTO(createdEntity, 0L);
    }

    /** 创建并安装初始版本
     * @param ragId 数据集ID
     * @param userId 用户ID */
    private void createAndInstallInitialVersion(String ragId, String userId) {
        // 创建0.0.1版本的发布请求
        PublishRagRequest publishRequest = new PublishRagRequest();
        publishRequest.setRagId(ragId);
        publishRequest.setVersion("0.0.1");
        publishRequest.setChangeLog("创建 RAG 默认版本");

        // 发布版本（保持为REVIEWING状态，不公开）
        RagVersionDTO versionDTO = ragPublishAppService.publishRagVersion(publishRequest, userId);

        // 使用新的自动安装方法（直接创建REFERENCE类型安装）
        userRagDomainService.autoInstallRag(userId, ragId, versionDTO.getId());

        log.info("Successfully created and auto-installed initial version 0.0.1 for dataset {} by user {}", ragId,
                userId);
    }

    /** 同步版本信息
     * @param datasetId 数据集ID
     * @param name 新名称
     * @param description 新描述
     * @param icon 新图标
     * @param userId 用户ID */
    private void syncVersionInfo(String datasetId, String name, String description, String icon, String userId) {
        // 查找对应的0.0.1版本
        RagVersionEntity version001 = ragVersionDomainService.findVersionByOriginalRagIdAndVersion(datasetId, "0.0.1",
                userId);

        if (version001 != null) {
            // 更新0.0.1版本的基本信息
            ragVersionDomainService.updateVersionBasicInfo(version001.getId(), name, description, icon, userId);
            log.debug("Successfully synced version 0.0.1 info for dataset {}", datasetId);
        } else {
            log.debug("Version 0.0.1 not found for dataset {}, skip sync", datasetId);
        }

        // 同步更新用户安装记录的基本信息（针对REFERENCE类型）
        try {
            userRagDomainService.updateUserRagBasicInfo(userId, datasetId, name, description, icon);
            log.debug("Successfully synced user installation info for dataset {}", datasetId);
        } catch (Exception e) {
            log.warn("Failed to sync user installation info for dataset {}: {}", datasetId, e.getMessage());
            // 不抛出异常，避免影响主流程
        }
    }

    /** 更新数据集
     * @param datasetId 数据集ID
     * @param request 更新请求
     * @param userId 用户ID
     * @return 数据集DTO */
    @Transactional(transactionManager = "transactionManager")
    public RagQaDatasetDTO updateDataset(String datasetId, UpdateDatasetRequest request, String userId) {
        RagQaDatasetEntity entity = RagQaDatasetAssembler.toEntity(request, datasetId, userId);
        ragQaDatasetDomainService.updateDataset(entity);

        // 同步更新对应的0.0.1版本信息（如果存在）
        try {
            syncVersionInfo(datasetId, request.getName(), request.getDescription(), request.getIcon(), userId);
        } catch (Exception e) {
            log.warn("Failed to sync version info for dataset {}: {}", datasetId, e.getMessage());
            // 不影响主流程，只记录警告
        }

        // 获取更新后的实体
        RagQaDatasetEntity updatedEntity = ragQaDatasetDomainService.getDataset(datasetId, userId);
        Long fileCount = fileDetailDomainService.countFilesByDataset(datasetId, userId);
        return RagQaDatasetAssembler.toDTO(updatedEntity, fileCount);
    }

    /** 删除数据集
     * @param datasetId 数据集ID
     * @param userId 用户ID */
    @Transactional(transactionManager = "transactionManager")
    public void deleteDataset(String datasetId, String userId) {
        // 参考工具删除逻辑：先获取版本历史，然后删除创建者自己的安装记录
        List<RagVersionDTO> versions = Collections.emptyList();
        try {
            versions = ragPublishAppService.getRagVersionHistory(datasetId, userId);
        } catch (Exception e) {
            log.debug("Failed to get version history for dataset {}, may not have versions", datasetId);
        }

        // 删除创建者自己在user_rags表中的安装记录（在删除数据集之前执行）
        try {
            // 按原始RAG ID删除安装记录，避免业务逻辑校验导致的事务回滚
            userRagDomainService.forceUninstallRagByOriginalId(userId, datasetId);
        } catch (Exception e) {
            // 如果没有安装记录，忽略异常
            log.debug("No installation record found for dataset {}", datasetId);
        }

        // 删除所有RAG发布版本
        for (RagVersionDTO version : versions) {
            try {
                ragVersionDomainService.deleteRagVersion(version.getId(), userId);
            } catch (Exception e) {
                // 如果版本不存在，忽略异常
                log.debug("Failed to delete RAG version {}, may not exist", version.getId());
            }
        }

        // 先删除数据集下的所有文件
        fileDetailDomainService.deleteAllFilesByDataset(datasetId, userId);

        // 再删除数据集
        ragQaDatasetDomainService.deleteDataset(datasetId, userId);
    }

    /** 获取数据集详情
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 数据集DTO */
    public RagQaDatasetDTO getDataset(String datasetId, String userId) {
        RagQaDatasetEntity entity = ragQaDatasetDomainService.getDataset(datasetId, userId);
        Long fileCount = fileDetailDomainService.countFilesByDataset(datasetId, userId);
        return RagQaDatasetAssembler.toDTO(entity, fileCount);
    }

    /** 获取数据集详情（用于Agent配置）
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 数据集DTO */
    public RagQaDatasetDTO getDatasetById(String datasetId, String userId) {
        return getDataset(datasetId, userId);
    }

    /** 获取用户可用的知识库详情（仅限已安装的知识库）
     * 
     * @param knowledgeBaseId 知识库ID（可能是originalRagId或userRagId）
     * @param userId 用户ID
     * @return 知识库详情
     * @throws BusinessException 如果知识库未安装或无权限访问 */
    public RagQaDatasetDTO getAvailableDatasetById(String knowledgeBaseId, String userId) {
        // 首先尝试作为userRagId查找已安装的知识库
        try {
            UserRagEntity userRag = userRagDomainService.getUserRag(userId, knowledgeBaseId);
            Long fileCount = getRagFileCount(userId, userRag);
            return RagQaDatasetAssembler.fromUserRagEntity(userRag, fileCount);
        } catch (Exception e) {
            // 如果不是userRagId，则尝试作为originalRagId查找已安装的知识库
            List<UserRagEntity> installedRags = userRagDomainService.listAllInstalledRags(userId);
            for (UserRagEntity userRag : installedRags) {
                if (knowledgeBaseId.equals(userRag.getOriginalRagId())) {
                    // 找到匹配的已安装知识库，返回快照信息
                    Long fileCount = getRagFileCount(userId, userRag);
                    return RagQaDatasetAssembler.fromUserRagEntity(userRag, fileCount);
                }
            }

            // 如果都没有找到，说明用户未安装该知识库，抛出异常
            throw new BusinessException("知识库未安装或无权限访问，请先安装该知识库");
        }
    }

    /** 批量获取数据集详情（用于Agent配置）
     * @param datasetIds 数据集ID列表
     * @param userId 用户ID
     * @return 数据集DTO列表 */
    public List<RagQaDatasetDTO> getDatasetsByIds(List<String> datasetIds, String userId) {
        List<RagQaDatasetDTO> datasets = new ArrayList<>();
        for (String datasetId : datasetIds) {
            try {
                RagQaDatasetDTO dataset = getDataset(datasetId, userId);
                datasets.add(dataset);
            } catch (Exception e) {
                log.warn("获取数据集 {} 失败，用户 {} 可能无权限访问: {}", datasetId, userId, e.getMessage());
                // 跳过无权限访问的数据集
            }
        }
        return datasets;
    }

    /** 获取用户可用的数据集列表（用于Agent配置） 只返回已安装的知识库（包括用户创建的和从市场安装的）
     * @param userId 用户ID
     * @return 数据集DTO列表 */
    public List<RagQaDatasetDTO> getUserAvailableDatasets(String userId) {
        List<RagQaDatasetDTO> availableDatasets = new ArrayList<>();

        // 获取用户所有已安装的RAG
        List<UserRagEntity> installedRags = userRagDomainService.listAllInstalledRags(userId);

        for (UserRagEntity userRag : installedRags) {
            try {
                // 根据RAG安装类型正确获取文件数量
                Long fileCount = getRagFileCount(userId, userRag);

                // 转换为DTO
                RagQaDatasetDTO dataset = RagQaDatasetAssembler.fromUserRagEntity(userRag, fileCount);
                availableDatasets.add(dataset);

            } catch (Exception e) {
                // 如果数据集不存在或无权限访问，跳过该安装记录
                log.warn("获取已安装RAG {} 失败，用户 {} 可能无权限访问或数据集已被删除: {}", userRag.getOriginalRagId(), userId,
                        e.getMessage());
            }
        }

        return availableDatasets;
    }

    /** 根据RAG安装类型获取正确的文件数量
     * 
     * @param userId 用户ID
     * @param userRag 用户RAG安装记录
     * @return 文件数量 */
    private Long getRagFileCount(String userId, UserRagEntity userRag) {
        if (userRag.isSnapshotType()) {
            // SNAPSHOT类型：统计用户快照文件数量
            return ragDataAccessService.countUserRagFiles(userRag.getId());
        } else {
            // REFERENCE类型：统计原始数据集文件数量（不进行用户权限检查，因为已安装表示有权限）
            return fileDetailDomainService.countFilesByDatasetWithoutUserCheck(userRag.getOriginalRagId());
        }
    }

    /** 分页查询数据集
     * @param request 查询请求
     * @param userId 用户ID
     * @return 分页结果 */
    public Page<RagQaDatasetDTO> listDatasets(QueryDatasetRequest request, String userId) {
        IPage<RagQaDatasetEntity> entityPage = ragQaDatasetDomainService.listDatasets(userId, request.getPage(),
                request.getPageSize(), request.getKeyword());

        Page<RagQaDatasetDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(),
                entityPage.getTotal());

        // 转换为DTO并添加文件数量
        List<RagQaDatasetDTO> dtoList = entityPage.getRecords().stream().map(entity -> {
            Long fileCount = fileDetailDomainService.countFilesByDataset(entity.getId(), userId);
            return RagQaDatasetAssembler.toDTO(entity, fileCount);
        }).toList();

        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    /** 获取所有数据集
     * @param userId 用户ID
     * @return 数据集列表 */
    public List<RagQaDatasetDTO> listAllDatasets(String userId) {
        List<RagQaDatasetEntity> entities = ragQaDatasetDomainService.listAllDatasets(userId);
        return entities.stream().map(entity -> {
            Long fileCount = fileDetailDomainService.countFilesByDataset(entity.getId(), userId);
            return RagQaDatasetAssembler.toDTO(entity, fileCount);
        }).toList();
    }

    /** 上传文件到数据集
     * @param request 上传请求
     * @param userId 用户ID
     * @return 文件DTO */
    @Transactional(transactionManager = "transactionManager")
    public FileDetailDTO uploadFile(UploadFileRequest request, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(request.getDatasetId(), userId);

        // 上传文件
        FileDetailEntity entity = FileDetailAssembler.toEntity(request, userId);
        FileDetailEntity uploadedEntity = fileDetailDomainService.uploadFileToDataset(entity);

        // 自动启动预处理流程
        autoStartPreprocessing(uploadedEntity.getId(), request.getDatasetId(), userId);

        return FileDetailAssembler.toDTO(uploadedEntity);
    }

    /** 自动启动预处理流程
     * @param fileId 文件ID
     * @param datasetId 数据集ID
     * @param userId 用户ID */
    private void autoStartPreprocessing(String fileId, String datasetId, String userId) {
        try {
            log.info("Auto-starting preprocessing for file: {}", fileId);

            // 清理已有的语料和向量数据
            cleanupExistingDocumentUnits(fileId);

            // 设置初始状态为初始化中
            // fileDetailDomainService.startFileOcrProcessing(fileId, userId);
            fileDetailDomainService.updateFileOcrProgress(fileId, 0, 0.0);
            // 重置向量化状态
            fileDetailDomainService.resetFileProcessing(fileId, userId);

            // 获取文件实体
            FileDetailEntity fileEntity = fileDetailDomainService.getFileByIdWithoutUserCheck(fileId);

            // 发送OCR处理MQ消息
            RagDocMessage ocrMessage = new RagDocMessage();
            ocrMessage.setFileId(fileId);
            ocrMessage.setPageSize(fileEntity.getFilePageSize());
            ocrMessage.setUserId(userId);
            // 获取用户的OCR模型配置并设置到消息中
            ocrMessage.setOcrModelConfig(userModelConfigResolver.getUserOcrModelConfig(userId));

            MessageEnvelope<RagDocMessage> envelope = MessageEnvelope.builder(ocrMessage)
                    .addEventType(EventType.DOC_REFRESH_ORG).description("文件自动预处理任务").build();
            messagePublisher.publish(RagDocSyncOcrEvent.route(), envelope);

            log.info("Auto-preprocessing started for file: {}", fileId);

        } catch (Exception e) {
            log.error("Failed to auto-start preprocessing for file: {}", fileId, e);
            // 如果自动启动失败，重置状态
            fileDetailDomainService.resetFileProcessing(fileId, userId);
        }
    }

    /** 删除数据集文件
     * @param datasetId 数据集ID
     * @param fileId 文件ID
     * @param userId 用户ID */
    @Transactional(transactionManager = "transactionManager")
    public void deleteFile(String datasetId, String fileId, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);

        // 删除文件
        fileDetailDomainService.deleteFile(fileId, userId);
    }

    /** 分页查询数据集文件
     * @param datasetId 数据集ID
     * @param request 查询请求
     * @param userId 用户ID
     * @return 分页结果 */
    public Page<FileDetailDTO> listDatasetFiles(String datasetId, QueryDatasetFileRequest request, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);

        IPage<FileDetailEntity> entityPage = fileDetailDomainService.listFilesByDataset(datasetId, userId,
                request.getPage(), request.getPageSize(), request.getKeyword());

        Page<FileDetailDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());

        List<FileDetailDTO> dtoList = FileDetailAssembler.toDTOs(entityPage.getRecords());
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    /** 获取数据集所有文件
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 文件列表 */
    public List<FileDetailDTO> listAllDatasetFiles(String datasetId, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);

        List<FileDetailEntity> entities = fileDetailDomainService.listAllFilesByDataset(datasetId, userId);
        return FileDetailAssembler.toDTOs(entities);
    }

    /** 启动文件预处理
     * @param request 预处理请求
     * @param userId 用户ID */
    @Transactional(transactionManager = "transactionManager")
    public void processFile(ProcessFileRequest request, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(request.getDatasetId(), userId);

        // 验证文件存在性和权限
        FileDetailEntity fileEntity = fileDetailDomainService.getFileById(request.getFileId(), userId);

        if (request.getProcessType() == 1) {
            // OCR预处理 - 检查是否可以启动预处理
            validateOcrProcessing(fileEntity);

            // 清理已有的语料和向量数据
            cleanupExistingDocumentUnits(request.getFileId());

            // fileDetailDomainService.startFileOcrProcessing(request.getFileId(), userId);
            fileDetailDomainService.updateFileOcrProgress(request.getFileId(), 0, 0.0);
            // 重置向量化状态
            fileDetailDomainService.resetFileProcessing(request.getFileId(), userId);

            // 发送OCR处理MQ消息
            RagDocMessage ocrMessage = new RagDocMessage();
            ocrMessage.setFileId(request.getFileId());
            ocrMessage.setPageSize(fileEntity.getFilePageSize());

            MessageEnvelope<RagDocMessage> envelope = MessageEnvelope.builder(ocrMessage)
                    .addEventType(EventType.DOC_REFRESH_ORG).description("文件OCR预处理任务").build();
            messagePublisher.publish(RagDocSyncOcrEvent.route(), envelope);

        } else if (request.getProcessType() == 2) {
            // 向量化处理 - 检查是否可以启动向量化
            validateEmbeddingProcessing(fileEntity);

            fileDetailDomainService.startFileEmbeddingProcessing(request.getFileId(), userId);
            fileDetailDomainService.updateFileEmbeddingProgress(request.getFileId(), 0, 0.0);

            List<DocumentUnitEntity> documentUnits = documentUnitDomainService
                    .listDocumentsByFileAndStatus(request.getFileId(), true, false);

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
                storageMessage.setDatasetId(request.getDatasetId()); // 设置数据集ID

                MessageEnvelope<RagDocSyncStorageMessage> env = MessageEnvelope.builder(storageMessage)
                        .addEventType(EventType.DOC_SYNC_RAG).description("文件向量化处理任务 - 页面 " + documentUnit.getPage())
                        .build();
                messagePublisher.publish(RagDocSyncStorageEvent.route(), env);
            }

        } else {
            throw new IllegalArgumentException("不支持的处理类型: " + request.getProcessType());
        }
    }

    /** 验证OCR预处理是否可以启动
     * @param fileEntity 文件实体 */
    private void validateOcrProcessing(FileDetailEntity fileEntity) {
        Integer processingStatus = fileEntity.getProcessingStatus();

        // 如果正在初始化，不能重复启动
        if (processingStatus != null && (processingStatus.equals(FileProcessingStatusEnum.OCR_PROCESSING.getCode())
                || processingStatus.equals(FileProcessingStatusEnum.EMBEDDING_PROCESSING.getCode()))) {
            throw new IllegalStateException("文件正在处理中，请等待处理完成");
        }
    }

    /** 验证向量化处理是否可以启动
     * @param fileEntity 文件实体 */
    private void validateEmbeddingProcessing(FileDetailEntity fileEntity) {
        Integer processingStatus = fileEntity.getProcessingStatus();

        // 必须先完成OCR处理
        if (processingStatus == null || (!processingStatus.equals(FileProcessingStatusEnum.OCR_COMPLETED.getCode())
                && !processingStatus.equals(FileProcessingStatusEnum.EMBEDDING_PROCESSING.getCode())
                && !processingStatus.equals(FileProcessingStatusEnum.COMPLETED.getCode())
                && !processingStatus.equals(FileProcessingStatusEnum.EMBEDDING_FAILED.getCode()))) {
            throw new IllegalStateException("文件需要先完成预处理才能进行向量化");
        }

        // 如果正在向量化，不能重复启动
        if (processingStatus.equals(FileProcessingStatusEnum.EMBEDDING_PROCESSING.getCode())) {
            throw new IllegalStateException("文件正在向量化中，请等待处理完成");
        }
    }

    /** 重新启动文件处理（强制重启，仅用于调试）
     * @param request 预处理请求
     * @param userId 用户ID */
    @Transactional(transactionManager = "transactionManager")
    public void reprocessFile(ProcessFileRequest request, String userId) {
        log.warn("Force reprocessing file: {}, type: {}, user: {}", request.getFileId(), request.getProcessType(),
                userId);

        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(request.getDatasetId(), userId);

        // 验证文件存在性和权限
        FileDetailEntity fileEntity = fileDetailDomainService.getFileById(request.getFileId(), userId);

        if (request.getProcessType() == 1) {
            // 强制重新OCR预处理
            log.info("Force restarting OCR preprocessing for file: {}", request.getFileId());

            // 清理已有的语料和向量数据
            cleanupExistingDocumentUnits(request.getFileId());

            // 重置状态
            // fileDetailDomainService.startFileOcrProcessing(request.getFileId(), userId);
            fileDetailDomainService.updateFileOcrProgress(request.getFileId(), 0, 0.0);
            // 也重置向量化状态
            fileDetailDomainService.resetFileProcessing(request.getFileId(), userId);

            // 发送OCR处理MQ消息
            RagDocMessage ocrMessage = new RagDocMessage();
            ocrMessage.setFileId(request.getFileId());
            ocrMessage.setPageSize(fileEntity.getFilePageSize());

            MessageEnvelope<RagDocMessage> envelope = MessageEnvelope.builder(ocrMessage)
                    .addEventType(EventType.DOC_REFRESH_ORG).description("文件强制重新OCR预处理任务").build();
            messagePublisher.publish(RagDocSyncOcrEvent.route(), envelope);

        } else if (request.getProcessType() == 2) {
            // 强制重新向量化处理
            log.info("Force restarting vectorization for file: {}", request.getFileId());

            // 检查是否已完成OCR处理
            Integer processingStatus = fileEntity.getProcessingStatus();
            if (processingStatus == null || (!processingStatus.equals(FileProcessingStatusEnum.OCR_COMPLETED.getCode())
                    && !processingStatus.equals(FileProcessingStatusEnum.EMBEDDING_PROCESSING.getCode())
                    && !processingStatus.equals(FileProcessingStatusEnum.COMPLETED.getCode())
                    && !processingStatus.equals(FileProcessingStatusEnum.EMBEDDING_FAILED.getCode()))) {
                throw new IllegalStateException("文件需要先完成预处理才能进行向量化");
            }

            // 重置向量化状态
            fileDetailDomainService.startFileEmbeddingProcessing(request.getFileId(), userId);

            List<DocumentUnitEntity> documentUnits = documentUnitDomainService
                    .listDocumentsByFileAndStatus(request.getFileId(), true, null);

            if (documentUnits.isEmpty()) {
                throw new IllegalStateException("文件没有找到可用于向量化的语料数据");
            }

            // 重置所有文档单元的向量化状态
            for (DocumentUnitEntity documentUnit : documentUnits) {
                documentUnit.setIsVector(false);
                documentUnitDomainService.updateDocumentUnitById(documentUnit);
            }

            // 为每个DocumentUnit发送向量化MQ消息
            for (DocumentUnitEntity documentUnit : documentUnits) {
                RagDocSyncStorageMessage storageMessage = new RagDocSyncStorageMessage();
                storageMessage.setId(documentUnit.getId());
                storageMessage.setFileId(request.getFileId());
                storageMessage.setFileName(fileEntity.getOriginalFilename());
                storageMessage.setPage(documentUnit.getPage());
                storageMessage.setContent(documentUnit.getContent());
                storageMessage.setVector(true);
                storageMessage.setDatasetId(request.getDatasetId());

                MessageEnvelope<RagDocSyncStorageMessage> env = MessageEnvelope.builder(storageMessage)
                        .addEventType(EventType.DOC_SYNC_RAG)
                        .description("文件强制重新向量化处理任务 - 页面 " + documentUnit.getPage()).build();
                messagePublisher.publish(RagDocSyncStorageEvent.route(), env);
            }

        } else {
            throw new IllegalArgumentException("不支持的处理类型: " + request.getProcessType());
        }
    }

    /** 清理文件的已有语料和向量数据
     * @param fileId 文件ID */
    private void cleanupExistingDocumentUnits(String fileId) {
        try {
            // 查询该文件的所有文档单元
            List<DocumentUnitEntity> existingUnits = documentUnitDomainService.listDocumentsByFile(fileId);

            if (!existingUnits.isEmpty()) {
                log.info("Cleaning up {} existing document units for file: {}", existingUnits.size(), fileId);

                final List<String> documentUnitEntities = Steam.of(existingUnits).map(DocumentUnitEntity::getId)
                        .toList();
                // 删除所有文档单元（包括语料和向量数据）
                documentUnitDomainService.batchDeleteDocumentUnits(documentUnitEntities);

                embeddingDomainService.deleteEmbedding(Collections.singletonList(fileId));
                log.info("Successfully cleaned up document units for file: {}", fileId);
            } else {
                log.debug("No existing document units found for file: {}", fileId);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup existing document units for file: {}", fileId, e);
            throw new BusinessException("清理已有语料数据失败: " + e.getMessage());
        }
    }

    /** 获取文件处理进度
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 处理进度 */
    public FileProcessProgressDTO getFileProgress(String fileId, String userId) {
        FileDetailEntity fileEntity = fileDetailDomainService.getFileById(fileId, userId);
        return FileProcessProgressAssembler.toDTO(fileEntity);
    }

    /** 获取数据集文件处理进度列表
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 处理进度列表 */
    public List<FileProcessProgressDTO> getDatasetFilesProgress(String datasetId, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);

        List<FileDetailEntity> entities = fileDetailDomainService.listAllFilesByDataset(datasetId, userId);
        return FileProcessProgressAssembler.toDTOs(entities);
    }
}
