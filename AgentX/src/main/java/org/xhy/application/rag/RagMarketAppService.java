package org.xhy.application.rag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.rag.assembler.RagVersionAssembler;
import org.xhy.application.rag.assembler.UserRagAssembler;
import org.xhy.application.rag.dto.RagMarketDTO;
import org.xhy.application.rag.dto.UserRagDTO;
import org.xhy.application.rag.request.InstallRagRequest;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.model.RagVersionEntity;
import org.xhy.domain.rag.model.UserRagEntity;
import org.xhy.domain.rag.service.RagQaDatasetDomainService;
import org.xhy.domain.rag.service.RagVersionDomainService;
import org.xhy.domain.rag.service.UserRagDomainService;
import org.xhy.domain.rag.service.UserRagSnapshotService;
import org.xhy.domain.user.service.UserDomainService;

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
    private final UserRagSnapshotService userRagSnapshotService;

    public RagMarketAppService(RagVersionDomainService ragVersionDomainService,
            UserRagDomainService userRagDomainService, UserDomainService userDomainService,
            RagQaDatasetDomainService ragQaDatasetDomainService, UserRagSnapshotService userRagSnapshotService) {
        this.ragVersionDomainService = ragVersionDomainService;
        this.userRagDomainService = userRagDomainService;
        this.userDomainService = userDomainService;
        this.ragQaDatasetDomainService = ragQaDatasetDomainService;
        this.userRagSnapshotService = userRagSnapshotService;
    }

    /** 获取市场上的RAG版本列表
     * 
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @param currentUserId 当前用户ID（用于判断是否已安装）
     * @return RAG市场列表 */
    public Page<RagMarketDTO> getMarketRagVersions(Integer page, Integer pageSize, String keyword,
            String currentUserId) {
        IPage<RagVersionEntity> entityPage = ragVersionDomainService.listPublishedVersions(page, pageSize, keyword);

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
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @return 用户安装的RAG列表 */
    public Page<UserRagDTO> getUserInstalledRags(String userId, Integer page, Integer pageSize, String keyword) {
        IPage<UserRagEntity> entityPage = userRagDomainService.listInstalledRags(userId, page, pageSize, keyword);

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
}