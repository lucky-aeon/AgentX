package org.xhy.application.rag.service.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.application.conversation.dto.AgentChatResponse;
import org.xhy.application.conversation.service.message.Agent;
import org.xhy.application.rag.assembler.DocumentUnitAssembler;
import org.xhy.application.rag.dto.*;
import org.xhy.application.conversation.dto.RagRetrievalDocumentDTO;
import org.xhy.application.rag.service.manager.RagModelConfigService;
import org.xhy.application.rag.service.manager.RagPublishAppService;
import org.xhy.application.rag.service.manager.RagQaDatasetAppService;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.llm.model.HighAvailabilityResult;
import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.llm.model.ProviderEntity;
import org.xhy.domain.llm.service.HighAvailabilityDomainService;
import org.xhy.domain.llm.service.LLMDomainService;
import org.xhy.domain.rag.service.DocumentUnitDomainService;
import org.xhy.domain.rag.service.EmbeddingDomainService;
import org.xhy.domain.rag.service.FileDetailDomainService;
import org.xhy.domain.rag.service.RagQaDatasetDomainService;
import org.xhy.domain.rag.service.management.RagDataAccessDomainService;
import org.xhy.domain.rag.service.management.RagVersionDomainService;
import org.xhy.domain.rag.service.management.UserRagDomainService;
import org.xhy.domain.rag.service.management.UserRagFileDomainService;
import org.xhy.domain.user.service.UserSettingsDomainService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.rag.factory.EmbeddingModelFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import org.xhy.domain.rag.model.UserRagFileEntity;
import org.xhy.domain.rag.model.ModelConfig;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.model.RagVersionEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.domain.rag.repository.UserRagFileRepository;
import org.xhy.domain.rag.dto.HybridSearchConfig;
import org.xhy.domain.rag.service.*;

import java.util.concurrent.CompletableFuture;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class RAGSearchAppService {

    private static final Logger log = LoggerFactory.getLogger(RagQaDatasetAppService.class);

    private final RagQaDatasetDomainService ragQaDatasetDomainService;
    private final FileDetailDomainService fileDetailDomainService;
    private final DocumentUnitDomainService documentUnitDomainService;
    private final EmbeddingDomainService embeddingDomainService;
    private final ObjectMapper objectMapper;
    private final LLMServiceFactory llmServiceFactory;
    private final LLMDomainService llmDomainService;
    private final UserSettingsDomainService userSettingsDomainService;
    private final HighAvailabilityDomainService highAvailabilityDomainService;

    private final UserRagDomainService userRagDomainService;
    private final RagDataAccessDomainService ragDataAccessService;
    private final RagModelConfigService ragModelConfigService;
    private final EmbeddingModelFactory embeddingModelFactory;
    private final UserRagFileRepository userRagFileRepository;
    private final HybridSearchDomainService hybridSearchDomainService;
    private final org.xhy.infrastructure.rag.service.UserModelConfigResolver userModelConfigResolver;
    private final UserRagFileDomainService userRagFileDomainService;

    public RAGSearchAppService(RagQaDatasetDomainService ragQaDatasetDomainService,
            FileDetailDomainService fileDetailDomainService, DocumentUnitDomainService documentUnitDomainService,
            EmbeddingDomainService embeddingDomainService, ObjectMapper objectMapper,
            LLMServiceFactory llmServiceFactory, LLMDomainService llmDomainService,
            UserSettingsDomainService userSettingsDomainService,
            HighAvailabilityDomainService highAvailabilityDomainService, RagPublishAppService ragPublishAppService,
            RagMarketAppService ragMarketAppService, RagVersionDomainService ragVersionDomainService,
            UserRagDomainService userRagDomainService, RagDataAccessDomainService ragDataAccessService,
            RagModelConfigService ragModelConfigService, EmbeddingModelFactory embeddingModelFactory,
            UserRagFileRepository userRagFileRepository, HybridSearchDomainService hybridSearchDomainService,
            org.xhy.infrastructure.rag.service.UserModelConfigResolver userModelConfigResolver) {
        this.ragQaDatasetDomainService = ragQaDatasetDomainService;
        this.fileDetailDomainService = fileDetailDomainService;
        this.documentUnitDomainService = documentUnitDomainService;
        this.embeddingDomainService = embeddingDomainService;
        this.objectMapper = objectMapper;
        this.llmServiceFactory = llmServiceFactory;
        this.llmDomainService = llmDomainService;
        this.userSettingsDomainService = userSettingsDomainService;
        this.highAvailabilityDomainService = highAvailabilityDomainService;
        this.userRagDomainService = userRagDomainService;
        this.ragDataAccessService = ragDataAccessService;
        this.ragModelConfigService = ragModelConfigService;
        this.embeddingModelFactory = embeddingModelFactory;
        this.userRagFileDomainService = userRagFileDomainService;
        this.userRagFileRepository = userRagFileRepository;
        this.hybridSearchDomainService = hybridSearchDomainService;
        this.userModelConfigResolver = userModelConfigResolver;
    }

    /** 创建数据集
     * @param request 创建请求
     * @param userId 用户ID
     * @return 数据集DTO */
    @Transactional
    public RagQaDatasetDTO createDataset(CreateDatasetRequest request, String userId) {
        RagQaDatasetEntity entity = RagQaDatasetAssembler.toEntity(request, userId);
        RagQaDatasetEntity createdEntity = ragQaDatasetDomainService.createDataset(entity);

        // 自动创建0.0.1版本并安装给用户
        try {
            createAndInstallInitialVersion(createdEntity.getId(), userId);
        } catch (Exception e) {
            log.warn("失败：创建数据集初始版本 {} 失败: {}", createdEntity.getId(), e.getMessage());
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

        log.info("成功为用户 {} 创建并自动安装了数据集 {} 的初始版本 0.0.1", userId, ragId);
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
            log.debug("成功同步了数据集 {} 的版本 0.0.1 信息", datasetId);
        } else {
            log.debug("没有找到数据集 {} 的版本 0.0.1，跳过同步", datasetId);
        }

        // 同步更新用户安装记录的基本信息（针对REFERENCE类型）
        try {
            userRagDomainService.updateUserRagBasicInfo(userId, datasetId, name, description, icon);
            log.debug("成功同步了数据集 {} 的用户安装信息", datasetId);
        } catch (Exception e) {
            log.warn("失败：同步数据集用户安装信息 {} 失败: {}", datasetId, e.getMessage());
            // 不抛出异常，避免影响主流程
        }
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

        // 同步更新对应的0.0.1版本信息（如果存在）
        try {
            syncVersionInfo(datasetId, request.getName(), request.getDescription(), request.getIcon(), userId);
        } catch (Exception e) {
            log.warn("失败：同步数据集版本信息 {} 失败: {}", datasetId, e.getMessage());
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
    @Transactional
    public void deleteDataset(String datasetId, String userId) {
        // 参考工具删除逻辑：先获取版本历史，然后删除创建者自己的安装记录
        List<RagVersionDTO> versions = Collections.emptyList();
        try {
            versions = ragPublishAppService.getRagVersionHistory(datasetId, userId);
        } catch (Exception e) {
            log.debug("失败：获取数据集版本历史 {}, 可能没有版本", datasetId);
        }

        // 删除创建者自己在user_rags表中的安装记录（在删除数据集之前执行）
        try {
            // 按原始RAG ID删除安装记录，避免业务逻辑校验导致的事务回滚
            userRagDomainService.forceUninstallRagByOriginalId(userId, datasetId);
        } catch (Exception e) {
            // 如果没有安装记录，忽略异常
            log.debug("没有找到数据集 {} 的安装记录", datasetId);
        }

        // 删除所有RAG发布版本
        for (RagVersionDTO version : versions) {
            try {
                ragVersionDomainService.deleteRagVersion(version.getId(), userId);
            } catch (Exception e) {
                // 如果版本不存在，忽略异常
                log.debug("失败：删除RAG版本 {}, 可能不存在", version.getId());
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
            log.info("自动启动文件预处理: {}", fileId);

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
            ocrMessage.setOcrModelConfig(ragModelConfigService.getUserOcrModelConfig(userId));

            RagDocSyncOcrEvent<RagDocMessage> ocrEvent = new RagDocSyncOcrEvent<>(ocrMessage,
                    EventType.DOC_REFRESH_ORG);
            ocrEvent.setDescription("文件自动预处理任务");
            applicationEventPublisher.publishEvent(ocrEvent);

            log.info("文件自动预处理已启动: {}", fileId);

        } catch (Exception e) {
            log.error("失败：自动启动文件预处理: {}", fileId, e);
            // 如果自动启动失败，重置状态
            fileDetailDomainService.resetFileProcessing(fileId, userId);
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

            // fileDetailDomainService.startFileOcrProcessing(request.getFileId(), userId);
            fileDetailDomainService.updateFileOcrProgress(request.getFileId(), 0, 0.0);
            // 重置向量化状态
            fileDetailDomainService.resetFileProcessing(request.getFileId(), userId);

            // 发送OCR处理MQ消息
            RagDocMessage ocrMessage = new RagDocMessage();
            ocrMessage.setFileId(request.getFileId());
            ocrMessage.setPageSize(fileEntity.getFilePageSize());

            RagDocSyncOcrEvent<RagDocMessage> ocrEvent = new RagDocSyncOcrEvent<>(ocrMessage,
                    EventType.DOC_REFRESH_ORG);
            ocrEvent.setDescription("文件OCR预处理任务");
            applicationEventPublisher.publishEvent(ocrEvent);

        } else if (request.getProcessType() == 2) {
            // 向量化处理 - 检查是否可以启动向量化
            validateEmbeddingProcessing(fileEntity);

            fileDetailDomainService.startFileEmbeddingProcessing(request.getFileId(), userId);
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
    @Transactional
    public void reprocessFile(ProcessFileRequest request, String userId) {
        log.warn("强制重新处理文件: {}，类型: {}，用户: {}", request.getFileId(), request.getProcessType(), userId);

        // 检查数据集是否存在
        ragQaDatasetDomainService.checkDatasetExists(request.getDatasetId(), userId);

        // 验证文件存在性和权限
        FileDetailEntity fileEntity = fileDetailDomainService.getFileById(request.getFileId(), userId);

        if (request.getProcessType() == 1) {
            // 强制重新OCR预处理
            log.info("强制重新启动OCR预处理文件: {}", request.getFileId());

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

            RagDocSyncOcrEvent<RagDocMessage> ocrEvent = new RagDocSyncOcrEvent<>(ocrMessage,
                    EventType.DOC_REFRESH_ORG);
            ocrEvent.setDescription("文件强制重新OCR预处理任务");
            applicationEventPublisher.publishEvent(ocrEvent);

        } else if (request.getProcessType() == 2) {
            // 强制重新向量化处理
            log.info("强制重新启动文件向量化处理: {}", request.getFileId());

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
            List<DocumentUnitEntity> existingUnits = documentUnitRepository
                    .selectList(Wrappers.<DocumentUnitEntity>lambdaQuery().eq(DocumentUnitEntity::getFileId, fileId));

            if (!existingUnits.isEmpty()) {
                log.info("清理文件 {} 的 {} 个现有文档单元", fileId, existingUnits.size());

                final List<String> documentUnitEntities = Steam.of(existingUnits).map(DocumentUnitEntity::getId)
                        .toList();
                // 删除所有文档单元（包括语料和向量数据）
                documentUnitRepository.deleteByIds(documentUnitEntities);

                embeddingDomainService.deleteEmbedding(Collections.singletonList(fileId));
                log.info("成功清理了文件 {} 的文档单元", fileId);
            } else {
                log.debug("没有找到文件 {} 的现有文档单元", fileId);
            }
        } catch (Exception e) {
            log.error("失败：清理文件现有文档单元: {}", fileId, e);
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

    /** RAG搜索文档（使用智能参数优化）
     * @param request 搜索请求
     * @param userId 用户ID
     * @return 搜索结果 */
    public List<DocumentUnitDTO> ragSearch(RagSearchRequest request, String userId) {
        // 验证数据集权限 - 检查用户是否安装了这些知识库
        List<String> validDatasetIds = new ArrayList<>();
        for (String datasetId : request.getDatasetIds()) {
            // 检查用户是否安装了这个知识库
            if (userRagDomainService.isRagInstalledByOriginalId(userId, datasetId)) {
                validDatasetIds.add(datasetId);
                log.debug("用户 {} 已安装知识库 {}，允许搜索", userId, datasetId);
            } else {
                // 检查用户是否是创建者（向后兼容）
                try {
                    ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);
                    validDatasetIds.add(datasetId);
                    log.debug("用户 {} 是知识库 {} 的创建者，允许搜索", userId, datasetId);
                } catch (Exception e) {
                    log.warn("用户 {} 既没有安装知识库 {} 也不是创建者，跳过搜索", userId, datasetId);
                }
            }
        }

        if (validDatasetIds.isEmpty()) {
            log.warn("用户 {} 没有任何有效的知识库可搜索", userId);
            return new ArrayList<>();
        }

        // 使用智能调整后的参数进行混合检索
        Double adjustedMinScore = request.getAdjustedMinScore();
        Integer adjustedCandidateMultiplier = request.getAdjustedCandidateMultiplier();

        // 获取用户的嵌入模型配置
        ModelConfig embeddingModelConfig = ragModelConfigService.getUserEmbeddingModelConfig(userId);
        EmbeddingModelFactory.EmbeddingConfig embeddingConfig = toEmbeddingConfig(embeddingModelConfig);

        // 获取用户的聊天模型配置用于HyDE
        ModelConfig chatModelConfig = null;
        try {
            // 使用UserModelConfigResolver获取聊天模型配置
            chatModelConfig = userModelConfigResolver.getUserChatModelConfig(userId);
            log.debug("获取用户 {} 的聊天模型配置成功，modelId: {}", userId, chatModelConfig.getModelId());
        } catch (Exception e) {
            log.warn("获取用户 {} 的聊天模型配置失败，HyDE功能将不可用: {}", userId, e.getMessage());
        }

        // 使用HybridSearchConfig配置对象调用混合检索服务
        HybridSearchConfig config = HybridSearchConfig.builder(validDatasetIds, request.getQuestion())
                .maxResults(request.getMaxResults()).minScore(adjustedMinScore) // 使用智能调整的相似度阈值
                .enableRerank(request.getEnableRerank()).candidateMultiplier(adjustedCandidateMultiplier) // 使用智能调整的候选结果倍数
                .embeddingConfig(embeddingConfig) // 传入嵌入模型配置
                .enableQueryExpansion(request.getEnableQueryExpansion()) // 传递查询扩展参数
                .chatModelConfig(chatModelConfig) // 传入聊天模型配置（用于HyDE）
                .build();
        List<DocumentUnitEntity> entities = hybridSearchDomainService.hybridSearch(config);

        // 转换为DTO并返回
        return DocumentUnitAssembler.toDTOs(entities);
    }

    /** 基于已安装知识库的RAG搜索
     *
     * @param request RAG搜索请求（使用userRagId作为数据源）
     * @param userRagId 用户已安装的RAG ID
     * @param userId 用户ID
     * @return 搜索结果 */
    public List<DocumentUnitDTO> ragSearchByUserRag(RagSearchRequest request, String userRagId, String userId) {
        // 获取RAG数据源信息
        RagDataAccessDomainService.RagDataSourceInfo sourceInfo = ragDataAccessService.getRagDataSourceInfo(userId,
                userRagId);

        // 根据安装类型获取实际的数据集ID
        String actualDatasetId;
        if (sourceInfo.getIsRealTime()) {
            // REFERENCE类型：使用原始数据集ID
            actualDatasetId = sourceInfo.getOriginalRagId();
        } else {
            // SNAPSHOT类型：使用原始数据集ID（但实际搜索会通过版本控制过滤）
            actualDatasetId = sourceInfo.getOriginalRagId();
        }

        // 验证数据集权限 - 通过userRagId已经验证了权限，不需要再检查用户是否是创建者
        // 只需要确认原始数据集仍然存在
        var originalDataset = ragQaDatasetDomainService.findDatasetById(actualDatasetId);
        if (originalDataset == null) {
            throw new BusinessException("原始数据集不存在或已被删除");
        }

        // 使用智能调整后的参数进行RAG搜索
        Double adjustedMinScore = request.getAdjustedMinScore();
        Integer adjustedCandidateMultiplier = request.getAdjustedCandidateMultiplier();

        // 获取用户的嵌入模型配置
        ModelConfig embeddingModelConfig = ragModelConfigService.getUserEmbeddingModelConfig(userId);
        EmbeddingModelFactory.EmbeddingConfig embeddingConfig = toEmbeddingConfig(embeddingModelConfig);

        // 获取用户的聊天模型配置用于HyDE
        ModelConfig chatModelConfig = null;
        try {
            // 使用UserModelConfigResolver获取聊天模型配置
            chatModelConfig = userModelConfigResolver.getUserChatModelConfig(userId);
            log.debug("ragSearchByUserRag - 获取用户 {} 的聊天模型配置成功，modelId: {}", userId, chatModelConfig.getModelId());
        } catch (Exception e) {
            log.warn("ragSearchByUserRag - 获取用户 {} 的聊天模型配置失败，HyDE功能将不可用: {}", userId, e.getMessage());
        }

        List<DocumentUnitEntity> entities;
        if (sourceInfo.getIsRealTime()) {
            // REFERENCE类型：使用混合检索搜索实时数据
            HybridSearchConfig config = HybridSearchConfig.builder(List.of(actualDatasetId), request.getQuestion())
                    .maxResults(request.getMaxResults()).minScore(adjustedMinScore)
                    .enableRerank(request.getEnableRerank()).candidateMultiplier(adjustedCandidateMultiplier)
                    .embeddingConfig(embeddingConfig).enableQueryExpansion(request.getEnableQueryExpansion())
                    .chatModelConfig(chatModelConfig) // 传入聊天模型配置（用于HyDE）
                    .build();
            entities = hybridSearchDomainService.hybridSearch(config);
        } else {
            // SNAPSHOT类型：搜索版本快照数据，使用混合检索
            List<DocumentUnitEntity> snapshotDocuments = ragDataAccessService.getRagDocuments(userId, userRagId);
            // 对快照数据进行混合检索（这里可能需要特殊处理，暂时使用相同逻辑）
            HybridSearchConfig config = HybridSearchConfig.builder(List.of(actualDatasetId), request.getQuestion())
                    .maxResults(request.getMaxResults()).minScore(adjustedMinScore)
                    .enableRerank(request.getEnableRerank()).candidateMultiplier(adjustedCandidateMultiplier)
                    .embeddingConfig(embeddingConfig).enableQueryExpansion(request.getEnableQueryExpansion())
                    .chatModelConfig(chatModelConfig) // 传入聊天模型配置（用于HyDE）
                    .build();
            entities = hybridSearchDomainService.hybridSearch(config);
        }

        // 转换为DTO并返回
        return DocumentUnitAssembler.toDTOs(entities);
    }

    /** RAG流式问答
     * @param request 流式问答请求
     * @param userId 用户ID
     * @return SSE流式响应 */
    public SseEmitter ragStreamChat(RagStreamChatRequest request, String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        AtomicBoolean completed = new AtomicBoolean(false);

        // 设置连接关闭回调
        emitter.onCompletion(() -> {
            completed.set(true);
            log.info("用户 {} 的RAG流式对话完成", userId);
        });
        emitter.onTimeout(() -> {
            log.warn("用户 {} 的RAG流式对话超时", userId);
            sendSseData(emitter, createErrorResponse("连接超时"), completed);
            safeCompleteEmitter(emitter, completed);
        });
        emitter.onError((ex) -> {
            log.error("用户 {} 的RAG流式对话连接错误", userId, ex);
            safeCompleteEmitter(emitter, completed);
        });

        // 异步处理流式问答
        CompletableFuture.runAsync(() -> {
            try {
                processRagStreamChat(request, userId, emitter, completed);
            } catch (Exception e) {
                log.error("RAG流式对话错误", e);
                sendSseData(emitter, createErrorResponse("处理过程中发生错误: " + e.getMessage()), completed);
            } finally {
                // 确保连接被正确关闭
                safeCompleteEmitter(emitter, completed);
            }
        });

        return emitter;
    }

    /** 处理RAG流式问答的核心逻辑 */
    private void processRagStreamChat(RagStreamChatRequest request, String userId, SseEmitter emitter,
            AtomicBoolean completed) {
        try {
            // 第一阶段：检索文档
            log.info("开始RAG流式对话 用户: {}, 问题: '{}'", userId, request.getQuestion());

            // 发送检索开始信号
            sendSseData(emitter, AgentChatResponse.build("开始检索相关文档...", MessageType.RAG_RETRIEVAL_START), completed);
            Thread.sleep(500);

            // 确定检索范围
            List<String> searchDatasetIds = new ArrayList<>();
            List<String> searchFileIds = new ArrayList<>();

            if (request.getFileId() != null && !request.getFileId().trim().isEmpty()) {
                FileDetailEntity fileEntity = fileDetailDomainService.getFileById(request.getFileId(), userId);
                searchFileIds.add(request.getFileId());
                searchDatasetIds.add(fileEntity.getDataSetId());
                sendSseData(emitter, AgentChatResponse.build("正在指定文件中检索...", MessageType.RAG_RETRIEVAL_PROGRESS),
                        completed);
            } else if (request.getDatasetIds() != null && !request.getDatasetIds().isEmpty()) {
                for (String datasetId : request.getDatasetIds()) {
                    ragQaDatasetDomainService.checkDatasetExists(datasetId, userId);
                }
                searchDatasetIds.addAll(request.getDatasetIds());
                sendSseData(emitter, AgentChatResponse.build("正在数据集中检索...", MessageType.RAG_RETRIEVAL_PROGRESS),
                        completed);
            } else {
                throw new IllegalArgumentException("必须指定文件ID或数据集ID");
            }

            // 获取用户的嵌入模型配置
            ModelConfig embeddingModelConfig = ragModelConfigService.getUserEmbeddingModelConfig(userId);
            EmbeddingModelFactory.EmbeddingConfig embeddingConfig = toEmbeddingConfig(embeddingModelConfig);

            // 执行RAG检索
            List<DocumentUnitEntity> retrievedDocuments;
            if (request.getFileId() != null && !request.getFileId().trim().isEmpty()) {
                retrievedDocuments = retrieveFromFile(request.getFileId(), request.getQuestion(),
                        request.getMaxResults(), embeddingConfig);
            } else {
                HybridSearchConfig config = HybridSearchConfig.builder(searchDatasetIds, request.getQuestion())
                        .maxResults(request.getMaxResults()).minScore(request.getMinScore())
                        .enableRerank(request.getEnableRerank()).candidateMultiplier(2).embeddingConfig(embeddingConfig)
                        .enableQueryExpansion(false) // 流式问答中暂时不启用查询扩展，保持现有行为
                        .build();
                retrievedDocuments = hybridSearchDomainService.hybridSearch(config);
            }

            // 构建检索结果
            List<RetrievedDocument> retrievedDocs = new ArrayList<>();
            for (DocumentUnitEntity doc : retrievedDocuments) {
                FileDetailEntity fileDetail = fileDetailDomainService.getFileById(doc.getFileId());
                double similarityScore = doc.getSimilarityScore() != null ? doc.getSimilarityScore() : 0.0;
                retrievedDocs.add(new RetrievedDocument(doc.getFileId(),
                        fileDetail != null ? fileDetail.getOriginalFilename() : "未知文件", doc.getId(), similarityScore));
            }

            // 发送检索完成信号
            String retrievalMessage = String.format("检索完成，找到 %d 个相关文档", retrievedDocs.size());
            AgentChatResponse retrievalEndResponse = AgentChatResponse.build(retrievalMessage,
                    MessageType.RAG_RETRIEVAL_END);
            try {
                retrievalEndResponse.setPayload(objectMapper.writeValueAsString(retrievedDocs));
            } catch (Exception e) {
                log.error("失败：序列化检索到的文档", e);
            }
            sendSseData(emitter, retrievalEndResponse, completed);
            Thread.sleep(1000);

            // 第二阶段：生成回答
            sendSseData(emitter, AgentChatResponse.build("开始生成回答...", MessageType.RAG_ANSWER_START), completed);
            Thread.sleep(500);

            // 构建LLM上下文
            String context = buildContextFromDocuments(retrievedDocuments);
            String prompt = buildRagPrompt(request.getQuestion(), context);

            // 调用流式LLM - 使用同步等待确保流式处理完成
            generateStreamAnswerAndWait(prompt, userId, emitter, completed);

            // 在LLM流式处理完成后发送完成信号
            sendSseData(emitter, AgentChatResponse.buildEndMessage("回答生成完成", MessageType.RAG_ANSWER_END), completed);

        } catch (Exception e) {
            log.error("RAG流式对话处理错误", e);
            sendSseData(emitter, createErrorResponse("处理过程中发生错误: " + e.getMessage()), completed);
        } finally {
            safeCompleteEmitter(emitter, completed);
        }
    }

    /** 从指定文件中检索相关文档 */
    private List<DocumentUnitEntity> retrieveFromFile(String fileId, String question, Integer maxResults,
            EmbeddingModelFactory.EmbeddingConfig embeddingConfig) {
        // 查询文件下的所有文档单元
        List<DocumentUnitEntity> fileDocuments = documentUnitDomainService.listVectorizedDocumentsByFile(fileId);

        if (fileDocuments.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用向量搜索在这些文档中检索
        FileDetailEntity fileEntity = fileDetailDomainService.getFileById(fileId);
        List<String> datasetIds = List.of(fileEntity.getDataSetId());

        HybridSearchConfig config = HybridSearchConfig.builder(datasetIds, question).maxResults(maxResults)
                .minScore(0.5).enableRerank(true).candidateMultiplier(2).embeddingConfig(embeddingConfig)
                .enableQueryExpansion(false) // 文件内检索暂时不启用查询扩展，保持现有行为
                .build();
        return hybridSearchDomainService.hybridSearch(config);
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
     * @param emitter SSE连接
     * @param completed 完成状态标志 */
    private void generateStreamAnswerAndWait(String prompt, String userId, SseEmitter emitter,
            AtomicBoolean completed) {
        try {
            log.info("开始生成RAG回答，用户: {}, 提示词长度: {}", userId, prompt.length());

            // 获取用户默认模型配置
            String userDefaultModelId = userSettingsDomainService.getUserDefaultModelId(userId);
            if (userDefaultModelId == null) {
                log.warn("用户 {} 未配置默认模型，使用临时简化响应", userId);
                generateMockStreamAnswer(emitter, completed);
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
                    sendSseData(emitter, AgentChatResponse.build("思考完成", MessageType.RAG_THINKING_END), completed);
                    thinkingEnded[0] = true;
                }

                // 如果没有思考过程且还没开始过思考，先发送思考开始和结束
                if (!hasThinkingProcess[0] && !thinkingStarted[0]) {
                    sendSseData(emitter, AgentChatResponse.build("开始思考...", MessageType.RAG_THINKING_START), completed);
                    sendSseData(emitter, AgentChatResponse.build("思考完成", MessageType.RAG_THINKING_END), completed);
                    thinkingStarted[0] = true;
                    thinkingEnded[0] = true;
                }

                sendSseData(emitter, AgentChatResponse.build(fragment, MessageType.RAG_ANSWER_PROGRESS), completed);
            }).onPartialReasoning(reasoning -> {

                // 标记有思考过程
                hasThinkingProcess[0] = true;

                // 如果还没开始思考，发送思考开始
                if (!thinkingStarted[0]) {
                    sendSseData(emitter, AgentChatResponse.build("开始思考...", MessageType.RAG_THINKING_START), completed);
                    thinkingStarted[0] = true;
                }

                // 发送思考进行中的状态（可选择是否发送思考内容）
                sendSseData(emitter, AgentChatResponse.build(reasoning, MessageType.RAG_THINKING_PROGRESS), completed);
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
                log.error("用户 {} 的RAG流式答案生成错误", userId, throwable);
                sendSseData(emitter, createErrorResponse("回答生成失败: " + throwable.getMessage()), completed);

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
                sendSseData(emitter, createErrorResponse("响应超时"), completed);
            } catch (Exception e) {
                log.error("等待LLM流式响应时发生错误，用户: {}", userId, e);
            }

        } catch (Exception e) {
            log.error("用户 {} 的RAG流式答案生成错误", userId, e);
            sendSseData(emitter, createErrorResponse("回答生成失败: " + e.getMessage()), completed);
        }
    }

    /** 生成模拟流式回答（备用方案） */
    private void generateMockStreamAnswer(SseEmitter emitter, AtomicBoolean completed) {
        try {
            // 模拟流式回答生成
            String[] responseFragments = {"根据检索到的文档内容，", "我可以为您提供以下回答：\n\n", "这是基于文档内容生成的回答。", "\n\n如需更详细的信息，",
                    "请提供更具体的问题。"};

            // 用于拼接完整回答
            StringBuilder fullMockAnswer = new StringBuilder();

            for (String fragment : responseFragments) {
                fullMockAnswer.append(fragment);
                sendSseData(emitter, AgentChatResponse.build(fragment, MessageType.RAG_ANSWER_PROGRESS), completed);
                Thread.sleep(200);
            }

            log.info("完整模拟RAG回答内容:\n{}", fullMockAnswer);

        } catch (Exception e) {
            log.error("生成模拟流式答案错误", e);
            sendSseData(emitter, createErrorResponse("回答生成失败: " + e.getMessage()), completed);
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

    /** 安全地完成SseEmitter，避免重复完成错误 */
    private void safeCompleteEmitter(SseEmitter emitter, AtomicBoolean completed) {
        if (completed.compareAndSet(false, true)) {
            try {
                emitter.complete();
                log.debug("SSE发射器成功完成");
            } catch (Exception e) {
                log.debug("SSE发射器完成错误（可能已经完成）: {}", e.getMessage());
            }
        } else {
            log.debug("SSE发射器已经完成，跳过");
        }
    }

    /** 发送SSE数据（带状态检查） */
    private void sendSseData(SseEmitter emitter, AgentChatResponse response, AtomicBoolean completed) {
        // 检查连接是否已经完成
        if (completed.get()) {
            return;
        }

        try {
            String jsonData = objectMapper.writeValueAsString(response);
            emitter.send(SseEmitter.event().data(jsonData));
        } catch (Exception e) {
            log.error("发送SSE数据失败", e);
            // 如果发送失败，标记为已完成避免后续操作
            safeCompleteEmitter(emitter, completed);
        }
    }

    /** 发送SSE数据（向后兼容方法） */
    private void sendSseData(SseEmitter emitter, AgentChatResponse response) {
        try {
            String jsonData = objectMapper.writeValueAsString(response);
            emitter.send(SseEmitter.event().data(jsonData));
        } catch (Exception e) {
            log.error("发送SSE数据失败", e);
        }
    }

    /** 创建错误响应 */
    private AgentChatResponse createErrorResponse(String errorMessage) {
        AgentChatResponse response = AgentChatResponse.buildEndMessage(errorMessage, MessageType.TEXT);
        return response;
    }

    /** 将ModelConfig转换为EmbeddingModelFactory.EmbeddingConfig
     *
     * @param modelConfig RAG模型配置
     * @return 嵌入模型工厂配置 */
    private EmbeddingModelFactory.EmbeddingConfig toEmbeddingConfig(ModelConfig modelConfig) {
        return new EmbeddingModelFactory.EmbeddingConfig(modelConfig.getApiKey(), modelConfig.getBaseUrl(),
                modelConfig.getModelId());
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

    /** 基于已安装知识库的RAG流式问答
     *
     * @param request 流式问答请求
     * @param userRagId 用户RAG安装记录ID
     * @param userId 用户ID
     * @return SSE流式响应 */
    public SseEmitter ragStreamChatByUserRag(RagStreamChatRequest request, String userRagId, String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        AtomicBoolean completed = new AtomicBoolean(false);

        // 设置连接关闭回调
        emitter.onCompletion(() -> {
            completed.set(true);
            log.info("用户 {} 通过userRag {} 的RAG流式对话完成", userId, userRagId);
        });
        emitter.onTimeout(() -> {
            log.warn("用户 {} 通过userRag {} 的RAG流式对话超时", userId, userRagId);
            sendSseData(emitter, createErrorResponse("连接超时"), completed);
            safeCompleteEmitter(emitter, completed);
        });
        emitter.onError((ex) -> {
            log.error("用户 {} 通过userRag {} 的RAG流式对话连接错误", userId, userRagId, ex);
            safeCompleteEmitter(emitter, completed);
        });

        // 异步处理流式问答
        CompletableFuture.runAsync(() -> {
            try {
                processRagStreamChatByUserRag(request, userRagId, userId, emitter, completed);
            } catch (Exception e) {
                log.error("通过userRag的RAG流式对话错误", e);
                sendSseData(emitter, createErrorResponse("处理过程中发生错误: " + e.getMessage()), completed);
            } finally {
                // 确保连接被正确关闭
                safeCompleteEmitter(emitter, completed);
            }
        });

        return emitter;
    }

    /** 处理基于用户RAG的流式问答核心逻辑 */
    private void processRagStreamChatByUserRag(RagStreamChatRequest request, String userRagId, String userId,
            SseEmitter emitter, AtomicBoolean completed) {
        try {
            // 检查用户RAG是否存在和有权限访问
            if (!ragDataAccessService.canAccessRag(userId, userRagId)) {
                sendSseData(emitter, createErrorResponse("处理过程中发生错误: 数据集不存在"), completed);
                return;
            }

            // 获取RAG数据源信息
            var dataSourceInfo = ragDataAccessService.getRagDataSourceInfo(userId, userRagId);
            log.info("开始RAG流式对话 通过userRag: {}, 用户: {}, 问题: '{}', 安装类型: {}", userRagId, userId, request.getQuestion(),
                    dataSourceInfo.getInstallType());

            // 第一阶段：检索文档
            sendSseData(emitter, AgentChatResponse.build("开始检索相关文档...", MessageType.RAG_RETRIEVAL_START), completed);
            Thread.sleep(500);

            // 获取用户的嵌入模型配置
            ModelConfig embeddingModelConfig = ragModelConfigService.getUserEmbeddingModelConfig(userId);
            EmbeddingModelFactory.EmbeddingConfig embeddingConfig = toEmbeddingConfig(embeddingModelConfig);

            List<DocumentUnitEntity> retrievedDocuments;

            // 根据RAG类型选择不同的数据源
            if (dataSourceInfo.getIsRealTime()) {
                // REFERENCE类型：使用原始RAG的数据集进行向量搜索
                List<String> ragDatasetIds = List.of(dataSourceInfo.getOriginalRagId());
                // 使用HybridSearchConfig配置对象调用混合检索
                HybridSearchConfig config = HybridSearchConfig.builder(ragDatasetIds, request.getQuestion())
                        .maxResults(request.getMaxResults()).minScore(request.getMinScore())
                        .enableRerank(request.getEnableRerank()).candidateMultiplier(2).embeddingConfig(embeddingConfig)
                        .enableQueryExpansion(false) // UserRag流式问答中暂时不启用查询扩展，保持现有行为
                        .build();
                retrievedDocuments = hybridSearchDomainService.hybridSearch(config);
            } else {
                // SNAPSHOT类型：使用用户快照数据进行检索
                retrievedDocuments = ragDataAccessService.getRagDocuments(userId, userRagId);

                // 如果快照数据为空，返回空结果
                if (retrievedDocuments.isEmpty()) {
                    log.info("用户RAG [{}] 的快照数据为空，无法进行检索", userRagId);
                } else {
                    // 对快照文档进行相关性过滤和排序
                    retrievedDocuments = filterAndRankSnapshotDocuments(retrievedDocuments, request.getQuestion(),
                            request.getMaxResults(), embeddingConfig);
                }
            }

            // 构建检索结果
            List<RetrievedDocument> retrievedDocs = new ArrayList<>();

            if (dataSourceInfo.getIsRealTime()) {
                // REFERENCE类型：使用原始文件信息
                for (DocumentUnitEntity doc : retrievedDocuments) {
                    FileDetailEntity fileDetail = fileDetailDomainService.getFileById(doc.getFileId());
                    double similarityScore = doc.getSimilarityScore() != null ? doc.getSimilarityScore() : 0.0;
                    retrievedDocs.add(new RetrievedDocument(doc.getFileId(),
                            fileDetail != null ? fileDetail.getOriginalFilename() : "未知文件", doc.getId(),
                            similarityScore));
                }
            } else {
                // SNAPSHOT类型：使用快照文件信息
                for (DocumentUnitEntity doc : retrievedDocuments) {
                    // doc.getFileId() 在SNAPSHOT模式下是 user_rag_files 的ID
                    UserRagFileEntity userFile = userRagFileDomainService.getById(doc.getFileId());
                    double similarityScore = doc.getSimilarityScore() != null ? doc.getSimilarityScore() : 0.0;
                    retrievedDocs.add(new RetrievedDocument(doc.getFileId(),
                            userFile != null ? userFile.getFileName() : "未知文件", doc.getId(), similarityScore));
                }
            }

            // 发送检索完成信号
            String retrievalMessage = String.format("检索完成，找到 %d 个相关文档", retrievedDocs.size());
            AgentChatResponse retrievalEndResponse = AgentChatResponse.build(retrievalMessage,
                    MessageType.RAG_RETRIEVAL_END);
            try {
                retrievalEndResponse.setPayload(objectMapper.writeValueAsString(retrievedDocs));
            } catch (Exception e) {
                log.error("失败：序列化检索到的文档", e);
            }
            sendSseData(emitter, retrievalEndResponse, completed);

            Thread.sleep(500);

            // 第二阶段：生成回答
            sendSseData(emitter, AgentChatResponse.build("开始生成回答...", MessageType.RAG_ANSWER_START), completed);
            Thread.sleep(500);

            // 构建LLM上下文
            String context = buildContextFromDocuments(retrievedDocuments);
            String prompt = buildRagPrompt(request.getQuestion(), context);

            // 调用流式LLM - 使用同步等待确保流式处理完成
            generateStreamAnswerAndWait(prompt, userId, emitter, completed);

            // 在LLM流式处理完成后发送完成信号
            sendSseData(emitter, AgentChatResponse.buildEndMessage("回答生成完成", MessageType.RAG_ANSWER_END), completed);

        } catch (Exception e) {
            log.error("processRagStreamChatByUserRag处理错误", e);
            sendSseData(emitter, createErrorResponse("处理过程中发生错误: " + e.getMessage()), completed);
        }
    }

    /** 对快照文档进行相关性过滤和排序
     *
     * @param documents 快照文档列表
     * @param question 用户问题
     * @param maxResults 最大返回数量
     * @param embeddingConfig 嵌入模型配置
     * @return 过滤和排序后的文档列表 */
    private List<DocumentUnitEntity> filterAndRankSnapshotDocuments(List<DocumentUnitEntity> documents, String question,
            Integer maxResults, EmbeddingModelFactory.EmbeddingConfig embeddingConfig) {

        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 获取嵌入模型
            EmbeddingModel embeddingModel = embeddingModelFactory.createEmbeddingModel(embeddingConfig);

            // 计算问题的向量
            Embedding questionEmbedding = embeddingModel.embed(question).content();

            // 为每个文档计算相似度
            List<DocumentWithScore> documentsWithScores = new ArrayList<>();
            for (DocumentUnitEntity doc : documents) {
                try {
                    // 计算文档内容的向量
                    Embedding docEmbedding = embeddingModel.embed(doc.getContent()).content();

                    // 计算余弦相似度
                    double similarity = cosineSimilarity(questionEmbedding.vectorAsList(), docEmbedding.vectorAsList());

                    // 设置相似度分数到文档实体
                    doc.setSimilarityScore(similarity);
                    documentsWithScores.add(new DocumentWithScore(doc, similarity));

                } catch (Exception e) {
                    log.warn("计算文档相似度失败: {}", e.getMessage());
                    // 如果计算失败，设置较低的相似度分数
                    doc.setSimilarityScore(0.0);
                    documentsWithScores.add(new DocumentWithScore(doc, 0.0));
                }
            }

            // 按相似度降序排序
            documentsWithScores.sort((a, b) -> Double.compare(b.score, a.score));

            // 限制返回数量
            int limit = maxResults != null
                    ? Math.min(maxResults, documentsWithScores.size())
                    : documentsWithScores.size();

            return documentsWithScores.stream().limit(limit).map(dws -> dws.document).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("快照文档相关性过滤失败", e);
            // 如果计算相似度失败，返回前N个文档
            int limit = maxResults != null ? Math.min(maxResults, documents.size()) : documents.size();
            return documents.subList(0, limit);
        }
    }

    /** 计算两个向量的余弦相似度 */
    private double cosineSimilarity(List<Float> vectorA, List<Float> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** 文档与分数的内部类 */
    private static class DocumentWithScore {
        final DocumentUnitEntity document;
        final double score;

        DocumentWithScore(DocumentUnitEntity document, double score) {
            this.document = document;
            this.score = score;
        }
    }

}
