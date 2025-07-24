package org.xhy.application.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.rag.assembler.DocumentUnitAssembler;
import org.xhy.application.rag.assembler.FileDetailAssembler;
import org.xhy.application.rag.assembler.RagVersionAssembler;
import org.xhy.application.rag.assembler.UserRagAssembler;
import org.xhy.application.rag.dto.DocumentUnitDTO;
import org.xhy.application.rag.dto.FileDetailDTO;
import org.xhy.application.rag.dto.RagMarketDTO;
import org.xhy.application.rag.dto.UserRagDTO;
import org.xhy.application.rag.request.InstallRagRequest;
import org.xhy.interfaces.dto.rag.request.QueryRagMarketRequest;
import org.xhy.interfaces.dto.rag.request.QueryUserInstalledRagRequest;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.model.RagVersionEntity;
import org.xhy.domain.rag.model.UserRagEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.service.FileDetailDomainService;
import org.xhy.domain.rag.service.RagDataAccessDomainService;
import org.xhy.domain.rag.service.RagQaDatasetDomainService;
import org.xhy.domain.rag.service.RagVersionDomainService;
import org.xhy.domain.rag.service.UserRagDomainService;
import org.xhy.domain.rag.service.UserRagSnapshotDomainService;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.domain.rag.constant.RagPublishStatus;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;

/** RAG市场应用服务
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@Service
public class RagMarketAppService {

    private final RagVersionDomainService ragVersionDomainService;
    private final UserRagDomainService userRagDomainService;
    private final UserDomainService userDomainService;
    private final RagQaDatasetDomainService ragQaDatasetDomainService;
    private final UserRagSnapshotDomainService userRagSnapshotService;
    private final RagDataAccessDomainService ragDataAccessService;
    private final FileDetailDomainService fileDetailDomainService;
    private final DocumentUnitRepository documentUnitRepository;

    public RagMarketAppService(RagVersionDomainService ragVersionDomainService,
            UserRagDomainService userRagDomainService, UserDomainService userDomainService,
            RagQaDatasetDomainService ragQaDatasetDomainService, UserRagSnapshotDomainService userRagSnapshotService,
            RagDataAccessDomainService ragDataAccessService, FileDetailDomainService fileDetailDomainService,
            DocumentUnitRepository documentUnitRepository) {
        this.ragVersionDomainService = ragVersionDomainService;
        this.userRagDomainService = userRagDomainService;
        this.userDomainService = userDomainService;
        this.ragQaDatasetDomainService = ragQaDatasetDomainService;
        this.userRagSnapshotService = userRagSnapshotService;
        this.ragDataAccessService = ragDataAccessService;
        this.fileDetailDomainService = fileDetailDomainService;
        this.documentUnitRepository = documentUnitRepository;
    }

    /** 获取市场上的RAG版本列表
     * 
     * @param request 查询请求
     * @param currentUserId 当前用户ID（用于判断是否已安装）
     * @return RAG市场列表 */
    public Page<RagMarketDTO> getMarketRagVersions(QueryRagMarketRequest request, String currentUserId) {
        IPage<RagVersionEntity> entityPage = ragVersionDomainService.listPublishedVersions(request.getPage(),
                request.getPageSize(), request.getKeyword());

        // 转换为MarketDTO
        List<RagMarketDTO> dtoList = RagVersionAssembler.toMarketDTOs(entityPage.getRecords());

        // 设置用户信息、安装次数和是否已安装
        for (RagMarketDTO dto : dtoList) {
            enrichWithUserInfo(dto);
            dto.setInstallCount(userRagDomainService.getInstallCount(dto.getId()));

            // 设置是否已安装
            if (StringUtils.isNotBlank(currentUserId)) {
                dto.setIsInstalled(userRagDomainService.isRagInstalled(currentUserId, dto.getId()));
            }
        }

        // 创建DTO分页对象
        Page<RagMarketDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    /** 安装RAG版本
     * 
     * @param request 安装请求
     * @param userId 用户ID
     * @return 安装后的RAG信息 */
    @Transactional
    public UserRagDTO installRagVersion(InstallRagRequest request, String userId) {
        // 安装RAG
        UserRagEntity userRag = userRagDomainService.installRag(userId, request.getRagVersionId());

        // 获取版本信息用于丰富DTO
        RagVersionEntity ragVersion = ragVersionDomainService.getRagVersion(request.getRagVersionId());

        // 转换为DTO并丰富信息
        UserRagDTO dto = UserRagAssembler.enrichWithVersionInfo(userRag, ragVersion.getOriginalRagId(),
                ragVersion.getFileCount(), ragVersion.getDocumentCount(), getUserNickname(ragVersion.getUserId()),
                ragVersion.getUserId());

        return dto;
    }

    /** 卸载RAG版本
     * 
     * @param ragVersionId RAG版本ID
     * @param userId 用户ID */
    @Transactional
    public void uninstallRagVersion(String ragVersionId, String userId) {
        userRagDomainService.uninstallRag(userId, ragVersionId);
    }

    /** 获取用户安装的RAG列表
     * 
     * @param userId 用户ID
     * @param request 查询请求
     * @return 用户安装的RAG列表 */
    public Page<UserRagDTO> getUserInstalledRags(String userId, QueryUserInstalledRagRequest request) {
        IPage<UserRagEntity> entityPage = userRagDomainService.listInstalledRags(userId, request.getPage(),
                request.getPageSize(), request.getKeyword());

        // 根据安装类型分别处理数据
        List<UserRagDTO> dtoList = new ArrayList<>();
        for (UserRagEntity entity : entityPage.getRecords()) {
            UserRagDTO dto;

            if (entity.isReferenceType()) {
                // REFERENCE类型：获取原始RAG的实时信息
                dto = enrichWithReferenceInfo(entity);
            } else {
                // SNAPSHOT类型：使用快照数据
                dto = enrichWithSnapshotInfo(entity);
            }

            dtoList.add(dto);
        }

        // 创建DTO分页对象
        Page<UserRagDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    /** 获取用户安装的所有RAG（用于对话中选择）
     * 
     * @param userId 用户ID
     * @return 用户安装的RAG列表 */
    public List<UserRagDTO> getUserAllInstalledRags(String userId) {
        List<UserRagEntity> entities = userRagDomainService.listAllInstalledRags(userId);

        // 根据安装类型分别处理数据
        List<UserRagDTO> dtoList = new ArrayList<>();
        for (UserRagEntity entity : entities) {
            UserRagDTO dto;

            if (entity.isReferenceType()) {
                // REFERENCE类型：获取原始RAG的实时信息
                dto = enrichWithReferenceInfo(entity);
            } else {
                // SNAPSHOT类型：使用快照数据
                dto = enrichWithSnapshotInfo(entity);
            }

            dtoList.add(dto);
        }

        return dtoList;
    }

    /** 获取用户安装的RAG详情
     * 
     * @param ragVersionId RAG版本ID
     * @param userId 用户ID
     * @return 安装的RAG详情 */
    public UserRagDTO getInstalledRagDetail(String ragVersionId, String userId) {
        UserRagEntity userRag = userRagDomainService.getInstalledRag(userId, ragVersionId);

        // 根据安装类型处理数据
        UserRagDTO dto;
        if (userRag.isReferenceType()) {
            // REFERENCE类型：获取原始RAG的实时信息
            dto = enrichWithReferenceInfo(userRag);
        } else {
            // SNAPSHOT类型：使用快照数据
            dto = enrichWithSnapshotInfo(userRag);
        }

        return dto;
    }

    /** 检查用户是否有权限使用RAG
     * 
     * @param userId 用户ID
     * @param ragId 原始RAG数据集ID
     * @param ragVersionId RAG版本ID
     * @return 是否有权限 */
    public boolean canUseRag(String userId, String ragId, String ragVersionId) {
        return userRagDomainService.canUseRag(userId, ragId, ragVersionId);
    }

    /** 切换已安装RAG的版本
     * 
     * @param userRagId 用户RAG安装记录ID
     * @param targetVersionId 目标版本ID
     * @param userId 用户ID
     * @return 切换后的RAG信息 */
    @Transactional
    public UserRagDTO switchRagVersion(String userRagId, String targetVersionId, String userId) {
        UserRagEntity updatedUserRag = userRagDomainService.switchRagVersion(userId, userRagId, targetVersionId);

        // 根据安装类型处理数据
        UserRagDTO dto;
        if (updatedUserRag.isReferenceType()) {
            // REFERENCE类型：获取原始RAG的实时信息
            dto = enrichWithReferenceInfo(updatedUserRag);
        } else {
            // SNAPSHOT类型：使用快照数据
            dto = enrichWithSnapshotInfo(updatedUserRag);
        }

        return dto;
    }

    /** 丰富用户信息
     * 
     * @param dto RAG市场DTO */
    private void enrichWithUserInfo(RagMarketDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getUserId())) {
            return;
        }

        try {
            var user = userDomainService.getUserInfo(dto.getUserId());
            if (user != null) {
                dto.setUserNickname(user.getNickname());
                dto.setUserAvatar(user.getAvatarUrl());
            }
        } catch (Exception e) {
            // 忽略用户查询异常
        }
    }

    /** 丰富版本信息
     * 
     * @param dto 用户RAG DTO */
    private void enrichWithVersionInfo(UserRagDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getRagVersionId())) {
            return;
        }

        try {
            RagVersionEntity ragVersion = ragVersionDomainService.getRagVersion(dto.getRagVersionId());
            if (ragVersion != null) {
                dto.setOriginalRagId(ragVersion.getOriginalRagId());
                dto.setFileCount(ragVersion.getFileCount());
                dto.setDocumentCount(ragVersion.getDocumentCount());
                dto.setCreatorNickname(getUserNickname(ragVersion.getUserId()));
                dto.setCreatorId(ragVersion.getUserId());
            }
        } catch (Exception e) {
            // 忽略版本查询异常
        }
    }

    /** 获取用户昵称
     * 
     * @param userId 用户ID
     * @return 用户昵称 */
    private String getUserNickname(String userId) {
        if (StringUtils.isBlank(userId)) {
            return null;
        }

        try {
            var user = userDomainService.getUserInfo(userId);
            return user != null ? user.getNickname() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 检查RAG版本是否已安装
     * 
     * @param ragVersionId RAG版本ID
     * @param userId 用户ID
     * @return 是否已安装 */
    public boolean isRagVersionInstalled(String ragVersionId, String userId) {
        return userRagDomainService.isRagInstalled(userId, ragVersionId);
    }

    // ========== 私有辅助方法 ==========

    /** 处理REFERENCE类型的信息丰富
     * 
     * @param entity 用户RAG实体
     * @return 丰富信息后的DTO */
    private UserRagDTO enrichWithReferenceInfo(UserRagEntity entity) {
        try {
            // 获取原始RAG的实时信息
            RagQaDatasetEntity originalRag = ragQaDatasetDomainService.getDataset(entity.getOriginalRagId(),
                    entity.getUserId());
            String creatorNickname = getUserNickname(originalRag.getUserId());

            return UserRagAssembler.enrichWithReferenceInfo(entity, originalRag, creatorNickname);
        } catch (Exception e) {
            // 如果原始RAG不存在，返回基本信息
            return UserRagAssembler.toDTO(entity);
        }
    }

    /** 处理SNAPSHOT类型的信息丰富
     * 
     * @param entity 用户RAG实体
     * @return 丰富信息后的DTO */
    private UserRagDTO enrichWithSnapshotInfo(UserRagEntity entity) {
        try {
            // 获取快照的统计信息（从用户快照表统计）
            Integer fileCount = userRagSnapshotService.getUserRagFileCount(entity.getId());
            Integer documentCount = userRagSnapshotService.getUserRagDocumentCount(entity.getId());

            // 获取创建者信息（尽量从版本信息获取，如果版本已删除则使用空值）
            String creatorNickname = null;
            String creatorId = null;
            try {
                RagVersionEntity ragVersion = ragVersionDomainService.getRagVersion(entity.getRagVersionId());
                creatorNickname = getUserNickname(ragVersion.getUserId());
                creatorId = ragVersion.getUserId();
            } catch (Exception e) {
                // 版本已删除，忽略创建者信息
            }

            return UserRagAssembler.enrichWithSnapshotInfo(entity, fileCount, documentCount, creatorNickname,
                    creatorId);
        } catch (Exception e) {
            return UserRagAssembler.toDTO(entity);
        }
    }

    /** 获取已安装RAG的文件列表（返回DTO）
     * 
     * @param userRagId 用户RAG安装记录ID
     * @param userId 用户ID
     * @return 文件DTO列表 */
    public List<FileDetailDTO> getInstalledRagFilesDTO(String userRagId, String userId) {
        List<FileDetailEntity> entities = ragDataAccessService.getRagFiles(userId, userRagId);
        return FileDetailAssembler.toDTOs(entities);
    }

    /** 获取已安装RAG的所有文档单元（返回DTO）
     * 
     * @param userRagId 用户RAG安装记录ID
     * @param userId 用户ID
     * @return 文档单元DTO列表 */
    public List<DocumentUnitDTO> getInstalledRagDocumentsDTO(String userRagId, String userId) {
        List<DocumentUnitEntity> entities = ragDataAccessService.getRagDocuments(userId, userRagId);
        return DocumentUnitAssembler.toDTOs(entities);
    }

    /** 获取已安装RAG特定文件的信息（返回DTO）
     * 
     * @param userRagId 用户RAG安装记录ID
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件详细信息DTO */
    public FileDetailDTO getInstalledRagFileInfoDTO(String userRagId, String fileId, String userId) {
        FileDetailEntity entity = ragDataAccessService.getRagFileInfo(userId, userRagId, fileId);
        return FileDetailAssembler.toDTO(entity);
    }

    /** 获取已安装RAG特定文件的文档单元（返回DTO）
     * 
     * @param userRagId 用户RAG安装记录ID
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文档单元DTO列表 */
    public List<DocumentUnitDTO> getInstalledRagFileDocumentsDTO(String userRagId, String fileId, String userId) {
        List<DocumentUnitEntity> entities = ragDataAccessService.getRagDocumentsByFile(userId, userRagId, fileId);
        return DocumentUnitAssembler.toDTOs(entities);
    }

    /** 获取用户安装的同一RAG的所有版本
     * 
     * @param userRagId 当前用户RAG安装记录ID
     * @param userId 用户ID
     * @return 同一原始RAG的所有可用版本列表（包括未安装的已发布版本） */
    public List<UserRagDTO> getInstalledRagVersions(String userRagId, String userId) {
        List<UserRagEntity> entities = userRagDomainService.getAvailableVersionsByUserRagId(userId, userRagId);

        // 根据安装类型分别处理数据
        List<UserRagDTO> dtoList = new ArrayList<>();
        for (UserRagEntity entity : entities) {
            UserRagDTO dto;

            if (entity.getId() == null) {
                // 虚拟的未安装版本，直接转换
                dto = UserRagAssembler.toDTO(entity);
            } else if (entity.isReferenceType()) {
                // REFERENCE类型：获取原始RAG的实时信息
                dto = enrichWithReferenceInfo(entity);
            } else {
                // SNAPSHOT类型：使用快照数据
                dto = enrichWithSnapshotInfo(entity);
            }

            dtoList.add(dto);
        }

        return dtoList;
    }

    /** 获取市场上RAG版本的文件列表（返回DTO）
     * 
     * @param ragVersionId RAG版本ID
     * @return 文件详细信息DTO列表 */
    public List<FileDetailDTO> getMarketRagFilesDTO(String ragVersionId) {
        // 根据版本ID获取原始RAG信息
        RagVersionEntity ragVersion = ragVersionDomainService.getRagVersion(ragVersionId);
        if (ragVersion == null) {
            return new ArrayList<>();
        }

        // 获取原始RAG的文件列表 - 注意：这里需要传入创建者的用户ID
        List<FileDetailEntity> entities = fileDetailDomainService.listAllFilesByDataset(ragVersion.getOriginalRagId(),
                ragVersion.getUserId());
        return FileDetailAssembler.toDTOs(entities);
    }

    /** 获取市场上RAG版本特定文件的文档单元（返回DTO）
     * 
     * @param ragVersionId RAG版本ID
     * @param fileId 文件ID
     * @return 文档单元DTO列表 */
    public List<DocumentUnitDTO> getMarketRagFileDocumentsDTO(String ragVersionId, String fileId) {
        // 调用统一的文档单元获取方法，不传用户ID表示市场访问
        return getRagFileDocuments(ragVersionId, fileId, null);
    }

    /** 统一的RAG文件文档单元获取方法
     * 
     * @param ragVersionId RAG版本ID
     * @param fileId 文件ID
     * @param userId 用户ID，null表示市场访问，非null表示已安装访问
     * @return 文档单元DTO列表 */
    private List<DocumentUnitDTO> getRagFileDocuments(String ragVersionId, String fileId, String userId) {
        // 获取RAG版本信息
        RagVersionEntity ragVersion = ragVersionDomainService.getRagVersion(ragVersionId);
        if (ragVersion == null) {
            return new ArrayList<>();
        }

        // 权限验证：市场访问需要验证发布状态和版本限制
        if (userId == null) {
            // 市场访问权限检查
            validateMarketAccess(ragVersion);
        } else {
            // 已安装访问权限检查（用户必须是创建者或已安装该版本）
            validateInstalledAccess(ragVersion, userId);
        }

        // 查询文档单元
        LambdaQueryWrapper<DocumentUnitEntity> wrapper = Wrappers.<DocumentUnitEntity>lambdaQuery()
                .eq(DocumentUnitEntity::getFileId, fileId).orderByAsc(DocumentUnitEntity::getPage);

        List<DocumentUnitEntity> entities = documentUnitRepository.selectList(wrapper);
        return DocumentUnitAssembler.toDTOs(entities);
    }

    /** 验证市场访问权限
     * 
     * @param ragVersion RAG版本实体 */
    private void validateMarketAccess(RagVersionEntity ragVersion) {
        // 1. 检查版本号：0.0.1版本不能公开访问
        if ("0.0.1".equals(ragVersion.getVersion())) {
            throw new BusinessException("该版本不对外开放");
        }

        // 2. 检查发布状态：只有已发布状态才能公开访问
        if (!RagPublishStatus.PUBLISHED.getCode().equals(ragVersion.getPublishStatus())) {
            throw new BusinessException("该RAG版本未发布或已下架");
        }
    }

    /** 验证已安装访问权限
     * 
     * @param ragVersion RAG版本实体
     * @param userId 用户ID */
    private void validateInstalledAccess(RagVersionEntity ragVersion, String userId) {
        // 已安装访问：创建者可以访问任何状态，其他用户只能访问已发布版本
        if (!ragVersion.getUserId().equals(userId)) {
            // 非创建者需要检查发布状态
            if (!RagPublishStatus.PUBLISHED.getCode().equals(ragVersion.getPublishStatus())) {
                throw new BusinessException("该RAG版本未发布或已下架");
            }
        }
        // 创建者可以访问自己创建的任何版本（包括0.0.1和未发布版本）
    }
}