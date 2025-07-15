package org.xhy.application.rag.service;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.data.message.SystemMessage;
import java.util.Arrays;
import java.util.Collections;
import org.dromara.streamquery.stream.core.stream.Steam;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.application.rag.assembler.DocumentUnitAssembler;
import org.xhy.application.rag.assembler.FileDetailAssembler;
import org.xhy.application.rag.assembler.RagQaDatasetAssembler;
import org.xhy.application.rag.dto.*;
import org.xhy.domain.rag.constant.FileInitializeStatus;
import org.xhy.domain.rag.constant.EmbeddingStatus;
import org.xhy.domain.rag.constant.MetadataConstant;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.message.RagDocSyncStorageMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.domain.rag.service.EmbeddingDomainService;
import org.xhy.domain.rag.service.FileDetailDomainService;
import org.xhy.domain.rag.service.RagQaDatasetDomainService;
import org.xhy.infrastructure.mq.enums.EventType;
import org.xhy.infrastructure.mq.events.RagDocSyncOcrEvent;
import org.xhy.infrastructure.mq.events.RagDocSyncStorageEvent;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.user.service.UserSettingsDomainService;
import org.xhy.domain.llm.model.HighAvailabilityResult;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import dev.langchain4j.data.message.UserMessage;
import org.xhy.application.conversation.service.message.Agent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.domain.conversation.constant.MessageType;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/** RAG数据集应用服务
 * @author shilong.zang
 * @date 2024-12-09 */
@Service
public class RagQaDatasetAppService {

    private static final Logger log = LoggerFactory.getLogger(RagQaDatasetAppService.class);

    private final RagQaDatasetDomainService ragQaDatasetDomainService;
    private final FileDetailDomainService fileDetailDomainService;
    private final DocumentUnitRepository documentUnitRepository;
    private final FileDetailRepository fileDetailRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final EmbeddingDomainService embeddingDomainService;
    private final ObjectMapper objectMapper;
    private final LLMServiceFactory llmServiceFactory;
    private final LLMDomainService llmDomainService;
    private final UserSettingsDomainService userSettingsDomainService;
    private final HighAvailabilityDomainService highAvailabilityDomainService;

    public RagQaDatasetAppService(RagQaDatasetDomainService ragQaDatasetDomainService,
            FileDetailDomainService fileDetailDomainService, DocumentUnitRepository documentUnitRepository,
            FileDetailRepository fileDetailRepository, ApplicationEventPublisher applicationEventPublisher,
            EmbeddingDomainService embeddingDomainService, ObjectMapper objectMapper,
            LLMServiceFactory llmServiceFactory, LLMDomainService llmDomainService,
            UserSettingsDomainService userSettingsDomainService,
            HighAvailabilityDomainService highAvailabilityDomainService) {
        this.ragQaDatasetDomainService = ragQaDatasetDomainService;
        this.fileDetailDomainService = fileDetailDomainService;
        this.documentUnitRepository = documentUnitRepository;
        this.fileDetailRepository = fileDetailRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.embeddingDomainService = embeddingDomainService;
        this.objectMapper = objectMapper;
        this.llmServiceFactory = llmServiceFactory;
        this.llmDomainService = llmDomainService;
        this.userSettingsDomainService = userSettingsDomainService;
        this.highAvailabilityDomainService = highAvailabilityDomainService;
    }

    /** 创建数据集
     * @param request 创建请求
     * @param userId 用户ID
     * @return 数据集DTO */
    @Transactional
    public RagQaDatasetDTO createDataset(CreateDatasetRequest request, String userId) {
        RagQaDatasetEntity entity = RagQaDatasetAssembler.toEntity(request, userId);
        RagQaDatasetEntity createdEntity = ragQaDatasetDomainService.createDataset(entity);
        return RagQaDatasetAssembler.toDTO(createdEntity, 0L);
    }

    /** 更新数据集
     * @param datasetId 数据集ID
     * @param request 更新请求
     * @param userId 用户ID
     * @return 数据集DTO */
    @Transactional
    public RagQaDatasetDTO updateDataset(String datasetId, UpdateDatasetRequest request, String userId) {
        RagQaDatasetEntity entity = RagQaDatasetAssembler.toEntity(request, datasetId, userId);
        ragQaDatasetDomainService.updateDataset(entity);

        // 获取更新后的实体
        RagQaDatasetEntity updatedEntity = ragQaDatasetDomainService.getDataset(datasetId, userId);
        Long fileCount = fileDetailDomainService.countFilesByDataset(datasetId, userId);
        return RagQaDatasetAssembler.toDTO(updatedEntity, fileCount);
    }

    /** 删除数据集
     * @param datasetId 数据集ID
     * @param userId 用户ID */
    @Transactional
    public void deleteDataset(String datasetId, String userId) {
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
    @Transactional
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
            fileDetailDomainService.updateFileInitializeStatus(fileId, FileInitializeStatus.INITIALIZING);
            fileDetailDomainService.updateFileOcrProgress(fileId, 0, 0.0);
            // 重置向量化状态
            fileDetailDomainService.updateFileEmbeddingStatus(fileId, EmbeddingStatus.UNINITIALIZED);
            fileDetailDomainService.updateFileEmbeddingProgress(fileId, 0, 0.0);

            // 获取文件实体
            FileDetailEntity fileEntity = fileDetailDomainService.getFileByIdWithoutUserCheck(fileId);

            // 发送OCR处理MQ消息
            RagDocSyncOcrMessage ocrMessage = new RagDocSyncOcrMessage();
            ocrMessage.setFileId(fileId);
            ocrMessage.setPageSize(fileEntity.getFilePageSize());

            RagDocSyncOcrEvent<RagDocSyncOcrMessage> ocrEvent = new RagDocSyncOcrEvent<>(ocrMessage,
                    EventType.DOC_REFRESH_ORG);
            ocrEvent.setDescription("文件自动预处理任务");
            applicationEventPublisher.publishEvent(ocrEvent);
            
            log.info("Auto-preprocessing started for file: {}", fileId);
            
        } catch (Exception e) {
            log.error("Failed to auto-start preprocessing for file: {}", fileId, e);
            // 如果自动启动失败，重置状态
            fileDetailDomainService.updateFileInitializeStatus(fileId, FileInitializeStatus.INITIALIZE_WAIT);
            fileDetailDomainService.updateFileOcrProgress(fileId, 0, 0.0);
        }
    }

    /** 删除数据集文件
     * @param datasetId 数据集ID
     * @param fileId 文件ID
     * @param userId 用户ID */
    @Transactional
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
    @Transactional
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
            
            fileDetailDomainService.updateFileInitializeStatus(request.getFileId(), FileInitializeStatus.INITIALIZING);
            fileDetailDomainService.updateFileOcrProgress(request.getFileId(), 0, 0.0);
            // 重置向量化状态
            fileDetailDomainService.updateFileEmbeddingStatus(request.getFileId(), EmbeddingStatus.UNINITIALIZED);
            fileDetailDomainService.updateFileEmbeddingProgress(request.getFileId(), 0, 0.0);

            // 发送OCR处理MQ消息
            RagDocSyncOcrMessage ocrMessage = new RagDocSyncOcrMessage();
            ocrMessage.setFileId(request.getFileId());
            ocrMessage.setPageSize(fileEntity.getFilePageSize());

            RagDocSyncOcrEvent<RagDocSyncOcrMessage> ocrEvent = new RagDocSyncOcrEvent<>(ocrMessage,
                    EventType.DOC_REFRESH_ORG);
            ocrEvent.setDescription("文件OCR预处理任务");
            applicationEventPublisher.publishEvent(ocrEvent);

        } else if (request.getProcessType() == 2) {
            // 向量化处理 - 检查是否可以启动向量化
            validateEmbeddingProcessing(fileEntity);

            fileDetailDomainService.updateFileEmbeddingStatus(request.getFileId(), EmbeddingStatus.INITIALIZING);
            fileDetailDomainService.updateFileEmbeddingProgress(request.getFileId(), 0, 0.0);

            List<DocumentUnitEntity> documentUnits = documentUnitRepository.selectList(Wrappers
                    .lambdaQuery(DocumentUnitEntity.class).eq(DocumentUnitEntity::getFileId, request.getFileId())
                    .eq(DocumentUnitEntity::getIsOcr, true).eq(DocumentUnitEntity::getIsVector, false));

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

                RagDocSyncStorageEvent<RagDocSyncStorageMessage> storageEvent = new RagDocSyncStorageEvent<>(
                        storageMessage, EventType.DOC_SYNC_RAG);
                storageEvent.setDescription("文件向量化处理任务 - 页面 " + documentUnit.getPage());
                applicationEventPublisher.publishEvent(storageEvent);
            }

        } else {
            throw new IllegalArgumentException("不支持的处理类型: " + request.getProcessType());
        }
    }

    /** 验证OCR预处理是否可以启动
     * @param fileEntity 文件实体 */
    private void validateOcrProcessing(FileDetailEntity fileEntity) {
        Integer initStatus = fileEntity.getIsInitialize();
        Integer embeddingStatus = fileEntity.getIsEmbedding();
        
        // 如果正在初始化，不能重复启动
        if (initStatus != null && initStatus.equals(FileInitializeStatus.INITIALIZING)) {
            throw new IllegalStateException("文件正在预处理中，请等待处理完成");
        }
        
        // 如果正在向量化，不能重新预处理
        if (embeddingStatus != null && embeddingStatus.equals(EmbeddingStatus.INITIALIZING)) {
            throw new IllegalStateException("文件正在向量化中，无法重新预处理");
        }
    }

    /** 验证向量化处理是否可以启动
     * @param fileEntity 文件实体 */
    private void validateEmbeddingProcessing(FileDetailEntity fileEntity) {
        Integer initStatus = fileEntity.getIsInitialize();
        Integer embeddingStatus = fileEntity.getIsEmbedding();
        
        // 必须先完成初始化
        if (initStatus == null || !initStatus.equals(FileInitializeStatus.INITIALIZED)) {
            throw new IllegalStateException("文件需要先完成预处理才能进行向量化");
        }
        
        // 如果正在向量化，不能重复启动
        if (embeddingStatus != null && embeddingStatus.equals(EmbeddingStatus.INITIALIZING)) {
            throw new IllegalStateException("文件正在向量化中，请等待处理完成");
        }
    }

    /** 重新启动文件处理（强制重启，仅用于调试）
     * @param request 预处理请求
     * @param userId 用户ID */
    @Transactional
    public void reprocessFile(ProcessFileRequest request, String userId) {
        log.warn("Force reprocessing file: {}, type: {}, user: {}", request.getFileId(), request.getProcessType(), userId);
        
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
            fileDetailDomainService.updateFileInitializeStatus(request.getFileId(), FileInitializeStatus.INITIALIZING);
            fileDetailDomainService.updateFileOcrProgress(request.getFileId(), 0, 0.0);
            // 也重置向量化状态
            fileDetailDomainService.updateFileEmbeddingStatus(request.getFileId(), EmbeddingStatus.UNINITIALIZED);
            fileDetailDomainService.updateFileEmbeddingProgress(request.getFileId(), 0, 0.0);

            // 发送OCR处理MQ消息
            RagDocSyncOcrMessage ocrMessage = new RagDocSyncOcrMessage();
            ocrMessage.setFileId(request.getFileId());
            ocrMessage.setPageSize(fileEntity.getFilePageSize());

            RagDocSyncOcrEvent<RagDocSyncOcrMessage> ocrEvent = new RagDocSyncOcrEvent<>(ocrMessage,
                    EventType.DOC_REFRESH_ORG);
            ocrEvent.setDescription("文件强制重新OCR预处理任务");
            applicationEventPublisher.publishEvent(ocrEvent);

        } else if (request.getProcessType() == 2) {
            // 强制重新向量化处理
            log.info("Force restarting vectorization for file: {}", request.getFileId());
            
            // 检查是否已完成初始化
            if (fileEntity.getIsInitialize() == null || !fileEntity.getIsInitialize().equals(FileInitializeStatus.INITIALIZED)) {
                throw new IllegalStateException("文件需要先完成预处理才能进行向量化");
            }

            // 重置向量化状态
            fileDetailDomainService.updateFileEmbeddingStatus(request.getFileId(), EmbeddingStatus.INITIALIZING);
            fileDetailDomainService.updateFileEmbeddingProgress(request.getFileId(), 0, 0.0);

            List<DocumentUnitEntity> documentUnits = documentUnitRepository.selectList(Wrappers
                    .lambdaQuery(DocumentUnitEntity.class).eq(DocumentUnitEntity::getFileId, request.getFileId())
                    .eq(DocumentUnitEntity::getIsOcr, true));

            if (documentUnits.isEmpty()) {
                throw new IllegalStateException("文件没有找到可用于向量化的语料数据");
            }

            // 重置所有文档单元的向量化状态
            for (DocumentUnitEntity documentUnit : documentUnits) {
                documentUnit.setIsVector(false);
                documentUnitRepository.updateById(documentUnit);
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

                RagDocSyncStorageEvent<RagDocSyncStorageMessage> storageEvent = new RagDocSyncStorageEvent<>(
                        storageMessage, EventType.DOC_SYNC_RAG);
                storageEvent.setDescription("文件强制重新向量化处理任务 - 页面 " + documentUnit.getPage());
                applicationEventPublisher.publishEvent(storageEvent);
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
            List<DocumentUnitEntity> existingUnits = documentUnitRepository.selectList(
                Wrappers.<DocumentUnitEntity>lambdaQuery()
                    .eq(DocumentUnitEntity::getFileId, fileId)
            );
            
            if (!existingUnits.isEmpty()) {
                log.info("Cleaning up {} existing document units for file: {}", existingUnits.size(), fileId);

                final List<String> documentUnitEntities = Steam.of(existingUnits).map(DocumentUnitEntity::getId).toList();
                // 删除所有文档单元（包括语料和向量数据）
                documentUnitRepository.deleteByIds(documentUnitEntities);

                embeddingDomainService.deleteEmbedding(Collections.singletonList(fileId));
                log.info("Successfully cleaned up document units for file: {}", fileId);
            } else {
                log.debug("No existing document units found for file: {}", fileId);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup existing document units for file: {}", fileId, e);
            throw new RuntimeException("清理已有语料数据失败: " + e.getMessage());
        }
    }

    /** 获取文件处理进度
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 处理进度 */
    public FileProcessProgressDTO getFileProgress(String fileId, String userId) {
        FileDetailEntity fileEntity = fileDetailDomainService.getFileById(fileId, userId);
        return buildFileProgressDTO(fileEntity);
    }

    /** 获取数据集文件处理进度列表
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 处理进度列表 */
    public List<FileProcessProgressDTO> getDatasetFilesProgress(String datasetId, String userId) {
        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);

        List<FileDetailEntity> entities = fileDetailDomainService.listAllFilesByDataset(datasetId, userId);
        return entities.stream().map(this::buildFileProgressDTO).toList();
    }

    /** 构建文件处理进度DTO
     * @param entity 文件实体
     * @return 处理进度DTO */
    private FileProcessProgressDTO buildFileProgressDTO(FileDetailEntity entity) {
        FileProcessProgressDTO dto = new FileProcessProgressDTO();
        dto.setFileId(entity.getId());
        dto.setFilename(entity.getOriginalFilename());
        
        // 设置新的枚举状态字段
        dto.setInitializeStatusEnum(org.xhy.domain.rag.constant.FileInitializeStatusEnum.fromCode(entity.getIsInitialize()));
        dto.setEmbeddingStatusEnum(org.xhy.domain.rag.constant.EmbeddingStatusEnum.fromCode(entity.getIsEmbedding()));
        
        // 设置中文状态字段（保持兼容性）
        dto.setInitializeStatus(org.xhy.domain.rag.constant.FileProcessStatusEnum.getInitStatusDescription(entity.getIsInitialize()));
        dto.setEmbeddingStatus(org.xhy.domain.rag.constant.FileProcessStatusEnum.getEmbeddingStatusDescription(entity.getIsEmbedding()));
        
        // 设置分离的页数和进度
        dto.setCurrentOcrPageNumber(entity.getCurrentOcrPageNumber() != null ? entity.getCurrentOcrPageNumber() : 0);
        dto.setCurrentEmbeddingPageNumber(entity.getCurrentEmbeddingPageNumber() != null ? entity.getCurrentEmbeddingPageNumber() : 0);
        dto.setFilePageSize(entity.getFilePageSize() != null ? entity.getFilePageSize() : 0);
        dto.setOcrProcessProgress(entity.getOcrProcessProgress() != null ? entity.getOcrProcessProgress() : 0.0);
        dto.setEmbeddingProcessProgress(entity.getEmbeddingProcessProgress() != null ? entity.getEmbeddingProcessProgress() : 0.0);
        
        // 设置兼容性字段
        dto.setIsInitialize(entity.getIsInitialize());
        dto.setIsEmbedding(entity.getIsEmbedding());
        dto.setCurrentPageNumber(entity.getCurrentOcrPageNumber() != null ? entity.getCurrentOcrPageNumber() : 0);
        dto.setProcessProgress(entity.getOcrProcessProgress() != null ? entity.getOcrProcessProgress() : 0.0);
        
        dto.setStatusDescription(getStatusDescription(entity));
        return dto;
    }

    /** 获取状态描述
     * @param entity 文件实体
     * @return 状态描述 */
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

    /** RAG搜索文档（使用智能参数优化）
     * @param request 搜索请求
     * @param userId 用户ID
     * @return 搜索结果 */
    public List<DocumentUnitDTO> ragSearch(RagSearchRequest request, String userId) {
        // 验证数据集权限 - 检查用户是否有这些数据集的访问权限
        for (String datasetId : request.getDatasetIds()) {
            ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);
        }

        // 使用智能调整后的参数进行RAG搜索
        Double adjustedMinScore = request.getAdjustedMinScore();
        Integer adjustedCandidateMultiplier = request.getAdjustedCandidateMultiplier();

        // 调用领域服务进行RAG搜索，使用智能优化的参数
        List<DocumentUnitEntity> entities = embeddingDomainService.ragDoc(request.getDatasetIds(),
                request.getQuestion(), request.getMaxResults(), adjustedMinScore, // 使用智能调整的相似度阈值
                request.getEnableRerank(), adjustedCandidateMultiplier // 使用智能调整的候选结果倍数
        );

        // 转换为DTO并返回
        return DocumentUnitAssembler.toDTOs(entities);
    }

    /** RAG流式问答
     * @param request 流式问答请求
     * @param userId 用户ID
     * @return SSE流式响应 */
    public SseEmitter ragStreamChat(RagStreamChatRequest request, String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // 设置连接关闭回调
        emitter.onCompletion(() -> log.info("RAG stream chat completed for user: {}", userId));
        emitter.onTimeout(() -> {
            log.warn("RAG stream chat timeout for user: {}", userId);
            sendSseData(emitter, createErrorResponse("连接超时"));
        });
        emitter.onError((ex) -> {
            log.error("RAG stream chat connection error for user: {}", userId, ex);
        });

        // 异步处理流式问答
        CompletableFuture.runAsync(() -> {
            try {
                processRagStreamChat(request, userId, emitter);
            } catch (Exception e) {
                log.error("RAG stream chat error", e);
                sendSseData(emitter, createErrorResponse("处理过程中发生错误: " + e.getMessage()));
            } finally {
                // 确保连接被正确关闭
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("Error completing SSE emitter", e);
                }
            }
        });

        return emitter;
    }

    /** 处理RAG流式问答的核心逻辑 */
    private void processRagStreamChat(RagStreamChatRequest request, String userId, SseEmitter emitter) {
        try {
            // 第一阶段：检索文档
            log.info("Starting RAG stream chat for user: {}, question: '{}'", userId, request.getQuestion());

            // 发送检索开始信号
            sendSseData(emitter, AgentChatResponse.build("开始检索相关文档...", MessageType.RAG_RETRIEVAL_START));
            Thread.sleep(500);

            // 确定检索范围
            List<String> searchDatasetIds = new ArrayList<>();
            List<String> searchFileIds = new ArrayList<>();

            if (request.getFileId() != null && !request.getFileId().trim().isEmpty()) {
                FileDetailEntity fileEntity = fileDetailDomainService.getFileById(request.getFileId(), userId);
                searchFileIds.add(request.getFileId());
                searchDatasetIds.add(fileEntity.getDataSetId());
                sendSseData(emitter, AgentChatResponse.build("正在指定文件中检索...", MessageType.RAG_RETRIEVAL_PROGRESS));
            } else if (request.getDatasetIds() != null && !request.getDatasetIds().isEmpty()) {
                for (String datasetId : request.getDatasetIds()) {
                    ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);
                }
                searchDatasetIds.addAll(request.getDatasetIds());
                sendSseData(emitter, AgentChatResponse.build("正在数据集中检索...", MessageType.RAG_RETRIEVAL_PROGRESS));
            } else {
                throw new IllegalArgumentException("必须指定文件ID或数据集ID");
            }

            // 执行RAG检索
            List<DocumentUnitEntity> retrievedDocuments;
            if (request.getFileId() != null && !request.getFileId().trim().isEmpty()) {
                retrievedDocuments = retrieveFromFile(request.getFileId(), request.getQuestion(),
                        request.getMaxResults());
            } else {
                retrievedDocuments = embeddingDomainService.ragDoc(searchDatasetIds, request.getQuestion(),
                        request.getMaxResults(), request.getMinScore(), request.getEnableRerank(), 2);
            }

            // 构建检索结果
            List<RetrievedDocument> retrievedDocs = new ArrayList<>();
            for (DocumentUnitEntity doc : retrievedDocuments) {
                FileDetailEntity fileDetail = fileDetailRepository.selectById(doc.getFileId());
                retrievedDocs.add(new RetrievedDocument(doc.getFileId(),
                        fileDetail != null ? fileDetail.getOriginalFilename() : "未知文件", doc.getId(), 0.85));
            }

            // 发送检索完成信号
            String retrievalMessage = String.format("检索完成，找到 %d 个相关文档", retrievedDocs.size());
            AgentChatResponse retrievalEndResponse = AgentChatResponse.build(retrievalMessage,
                    MessageType.RAG_RETRIEVAL_END);
            try {
                retrievalEndResponse.setPayload(objectMapper.writeValueAsString(retrievedDocs));
            } catch (Exception e) {
                log.error("Failed to serialize retrieved documents", e);
            }
            sendSseData(emitter, retrievalEndResponse);
            Thread.sleep(1000);

            // 第二阶段：生成回答
            sendSseData(emitter, AgentChatResponse.build("开始生成回答...", MessageType.RAG_ANSWER_START));
            Thread.sleep(500);

            // 构建LLM上下文
            String context = buildContextFromDocuments(retrievedDocuments);
            String prompt = buildRagPrompt(request.getQuestion(), context);

            // 调用流式LLM - 使用同步等待确保流式处理完成
            generateStreamAnswerAndWait(prompt, userId, emitter);

            // 在LLM流式处理完成后发送完成信号
            sendSseData(emitter, AgentChatResponse.buildEndMessage("回答生成完成", MessageType.RAG_ANSWER_END));

        } catch (Exception e) {
            log.error("Error in RAG stream chat processing", e);
            sendSseData(emitter, createErrorResponse("处理过程中发生错误: " + e.getMessage()));
        } finally {
            emitter.complete();
        }
    }

    /** 从指定文件中检索相关文档 */
    private List<DocumentUnitEntity> retrieveFromFile(String fileId, String question, Integer maxResults) {
        // 查询文件下的所有文档单元
        List<DocumentUnitEntity> fileDocuments = documentUnitRepository
                .selectList(Wrappers.lambdaQuery(DocumentUnitEntity.class).eq(DocumentUnitEntity::getFileId, fileId)
                        .eq(DocumentUnitEntity::getIsVector, true));

        if (fileDocuments.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取文档ID列表
        List<String> documentIds = fileDocuments.stream().map(DocumentUnitEntity::getId).collect(Collectors.toList());

        // 使用向量搜索在这些文档中检索
        FileDetailEntity fileEntity = fileDetailRepository.selectById(fileId);
        List<String> datasetIds = List.of(fileEntity.getDataSetId());

        return embeddingDomainService.ragDoc(datasetIds, question, maxResults, 0.5, true, 2);
    }

    /** 构建检索文档的上下文 */
    private String buildContextFromDocuments(List<DocumentUnitEntity> documents) {
        if (documents.isEmpty()) {
            return "暂无相关文档信息。";
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是相关的文档片段：\n\n");

        for (int i = 0; i < documents.size(); i++) {
            DocumentUnitEntity doc = documents.get(i);
            context.append(String.format("文档片段 %d：\n", i + 1));
            context.append(doc.getContent());
            context.append("\n\n");
        }

        return context.toString();
    }

    /** 构建RAG提示词 */
    private String buildRagPrompt(String question, String context) {
        return String.format(
                "请基于以下提供的文档内容回答用户的问题。如果文档中没有相关信息，请诚实地告知用户。\n\n" + "文档内容：\n%s\n\n" + "用户问题：%s\n\n" + "请提供准确、有帮助的回答：",
                context, question);
    }

    /** 生成流式回答并等待完成
     * @param prompt RAG提示词
     * @param userId 用户ID
     * @param emitter SSE连接 */
    private void generateStreamAnswerAndWait(String prompt, String userId, SseEmitter emitter) {
        try {
            log.info("开始生成RAG回答，用户: {}, 提示词长度: {}", userId, prompt.length());

            // 获取用户默认模型配置
            String userDefaultModelId = userSettingsDomainService.getUserDefaultModelId(userId);
            if (userDefaultModelId == null) {
                log.warn("用户 {} 未配置默认模型，使用临时简化响应", userId);
                generateMockStreamAnswer(emitter);
                return;
            }

            ModelEntity model = llmDomainService.getModelById(userDefaultModelId);
            List<String> fallbackChain = userSettingsDomainService.getUserFallbackChain(userId);

            // 获取最佳服务商（支持高可用、降级）
            HighAvailabilityResult result = highAvailabilityDomainService.selectBestProvider(model, userId,
                    "rag-session-" + userId, fallbackChain);
            ProviderEntity provider = result.getProvider();
            ModelEntity selectedModel = result.getModel();

            // 创建流式LLM客户端
            StreamingChatModel streamingClient = llmServiceFactory.getStreamingClient(provider, selectedModel);

            // 创建Agent并启动流式处理
            Agent agent = buildStreamingAgent(streamingClient);
            TokenStream tokenStream = agent.chat(prompt);

            // 记录调用开始时间
            long startTime = System.currentTimeMillis();

            // 使用CompletableFuture来等待流式处理完成
            CompletableFuture<Void> streamComplete = new CompletableFuture<>();

            // 思维链状态跟踪
            final boolean[] thinkingStarted = {false};
            final boolean[] thinkingEnded = {false};
            final boolean[] hasThinkingProcess = {false};

            // 普通模型的流式处理方式
            tokenStream.onPartialResponse(fragment -> {
                log.debug("收到响应片段: {}", fragment);

                // 如果有思考过程但还没结束思考，先结束思考阶段
                if (hasThinkingProcess[0] && !thinkingEnded[0]) {
                    sendSseData(emitter, AgentChatResponse.build("思考完成", MessageType.RAG_THINKING_END));
                    thinkingEnded[0] = true;
                }

                // 如果没有思考过程且还没开始过思考，先发送思考开始和结束
                if (!hasThinkingProcess[0] && !thinkingStarted[0]) {
                    sendSseData(emitter, AgentChatResponse.build("开始思考...", MessageType.RAG_THINKING_START));
                    sendSseData(emitter, AgentChatResponse.build("思考完成", MessageType.RAG_THINKING_END));
                    thinkingStarted[0] = true;
                    thinkingEnded[0] = true;
                }

                sendSseData(emitter, AgentChatResponse.build(fragment, MessageType.RAG_ANSWER_PROGRESS));
            }).onPartialReasoning(reasoning -> {
                log.debug("收到思维链片段: {}", reasoning);

                // 标记有思考过程
                hasThinkingProcess[0] = true;

                // 如果还没开始思考，发送思考开始
                if (!thinkingStarted[0]) {
                    sendSseData(emitter, AgentChatResponse.build("开始思考...", MessageType.RAG_THINKING_START));
                    thinkingStarted[0] = true;
                }

                // 发送思考进行中的状态（可选择是否发送思考内容）
                sendSseData(emitter, AgentChatResponse.build(reasoning, MessageType.RAG_THINKING_PROGRESS));
            }).onCompleteReasoning(completeReasoning -> {
                log.info("思维链生成完成，长度: {}", completeReasoning.length());
                log.info("完整思维链内容:\n{}", completeReasoning);
            }).onCompleteResponse(chatResponse -> {
                String fullAnswer = chatResponse.aiMessage().text();
                log.info("RAG回答生成完成，用户: {}, 响应长度: {}", userId, fullAnswer.length());
                log.info("完整RAG回答内容:\n{}", fullAnswer);

                // 上报调用成功结果
                long latency = System.currentTimeMillis() - startTime;
                highAvailabilityDomainService.reportCallResult(result.getInstanceId(), selectedModel.getId(), true,
                        latency, null);

                streamComplete.complete(null);
            }).onError(throwable -> {
                log.error("RAG stream answer generation error for user: {}", userId, throwable);
                sendSseData(emitter, createErrorResponse("回答生成失败: " + throwable.getMessage()));

                long latency = System.currentTimeMillis() - startTime;
                highAvailabilityDomainService.reportCallResult(result.getInstanceId(), selectedModel.getId(), false,
                        latency, throwable.getMessage());

                streamComplete.completeExceptionally(throwable);
            });

            // 启动流处理
            tokenStream.start();

            // 等待流式处理完成，最多等待30分钟
            try {
                streamComplete.get(30, java.util.concurrent.TimeUnit.MINUTES);
            } catch (java.util.concurrent.TimeoutException e) {
                log.warn("LLM流式响应超时，用户: {}", userId);
                sendSseData(emitter, createErrorResponse("响应超时"));
            } catch (Exception e) {
                log.error("等待LLM流式响应时发生错误，用户: {}", userId, e);
            }

        } catch (Exception e) {
            log.error("Error in RAG stream answer generation for user: {}", userId, e);
            sendSseData(emitter, createErrorResponse("回答生成失败: " + e.getMessage()));
        }
    }

    /** 生成模拟流式回答（备用方案） */
    private void generateMockStreamAnswer(SseEmitter emitter) {
        try {
            // 模拟流式回答生成
            String[] responseFragments = {"根据检索到的文档内容，", "我可以为您提供以下回答：\n\n", "这是基于文档内容生成的回答。", "\n\n如需更详细的信息，",
                    "请提供更具体的问题。"};

            // 用于拼接完整回答
            StringBuilder fullMockAnswer = new StringBuilder();

            for (String fragment : responseFragments) {
                fullMockAnswer.append(fragment);
                sendSseData(emitter, AgentChatResponse.build(fragment, MessageType.RAG_ANSWER_PROGRESS));
                Thread.sleep(200);
            }

            log.info("完整模拟RAG回答内容:\n{}", fullMockAnswer);

        } catch (Exception e) {
            log.error("Error generating mock stream answer", e);
            sendSseData(emitter, createErrorResponse("回答生成失败: " + e.getMessage()));
        }
    }

    /** 构建流式Agent */
    private Agent buildStreamingAgent(StreamingChatModel streamingClient) {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder().maxMessages(10)
                .chatMemoryStore(new InMemoryChatMemoryStore()).build();
        memory.add(new SystemMessage("""
                    你是一位专业的文档问答助手，你的任务是基于提供的文档回答用户问题。
                你需要遵循以下Markdown格式要求：
                1. 使用标准Markdown语法
                2. 列表项使用 ' - ' 而不是 '*'，确保破折号后有一个空格
                3. 引用页码使用方括号，例如：[页码: 1]
                4. 在每个主要段落之间添加一个空行
                5. 加粗使用 **文本** 格式
                6. 保持一致的缩进，列表项不要过度缩进
                7. 确保列表项之间没有多余的空行
                8. 该加## 这种标题的时候要加上

                回答结构应该是：
                1. 首先是简短的介绍语
                2. 然后是主要内容（使用列表形式）
                3. 最后是"信息来源"部分，总结使用的页面及其贡献
                """));

        return AiServices.builder(Agent.class).streamingChatModel(streamingClient).chatMemory(memory).build();
    }

    /** 发送SSE数据（带状态检查） */
    private void sendSseData(SseEmitter emitter, AgentChatResponse response) {
        try {
            String jsonData = objectMapper.writeValueAsString(response);
            emitter.send(SseEmitter.event().data(jsonData));
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("already completed")) {
                log.warn("SSE连接已关闭，跳过数据发送: {}", response.getMessageType());
            } else {
                log.error("SSE状态错误", e);
            }
        } catch (Exception e) {
            log.error("发送SSE数据失败", e);
        }
    }

    /** 创建错误响应 */
    private AgentChatResponse createErrorResponse(String errorMessage) {
        AgentChatResponse response = AgentChatResponse.buildEndMessage(errorMessage, MessageType.TEXT);
        return response;
    }

    /** 检索到的文档信息（内部类） */
    private static class RetrievedDocument {
        private String fileId;
        private String fileName;
        private String documentId;
        private Double score;

        public RetrievedDocument() {
        }

        public RetrievedDocument(String fileId, String fileName, String documentId, Double score) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.documentId = documentId;
            this.score = score;
        }

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getDocumentId() {
            return documentId;
        }

        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }
    }
}