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
import org.xhy.domain.rag.model.RagVersionEntity;
import org.xhy.domain.rag.model.UserRagEntity;
import org.xhy.domain.rag.service.RagVersionDomainService;
import org.xhy.domain.rag.service.UserRagDomainService;
import org.xhy.domain.user.service.UserDomainService;

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

    public RagMarketAppService(RagVersionDomainService ragVersionDomainService,
            UserRagDomainService userRagDomainService, UserDomainService userDomainService) {
        this.ragVersionDomainService = ragVersionDomainService;
        this.userRagDomainService = userRagDomainService;
        this.userDomainService = userDomainService;
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

        // 转换为DTO
        List<UserRagDTO> dtoList = UserRagAssembler.toDTOs(entityPage.getRecords());

        // 丰富版本信息
        for (UserRagDTO dto : dtoList) {
            enrichWithVersionInfo(dto);
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

        // 转换为DTO
        List<UserRagDTO> dtoList = UserRagAssembler.toDTOs(entities);

        // 丰富版本信息
        for (UserRagDTO dto : dtoList) {
            enrichWithVersionInfo(dto);
        }

        return dtoList;
    }

    /** 更新安装的RAG状态
     * 
     * @param ragVersionId RAG版本ID
     * @param isActive 是否激活
     * @param userId 用户ID */
    @Transactional
    public void updateRagStatus(String ragVersionId, boolean isActive, String userId) {
        userRagDomainService.updateRagStatus(userId, ragVersionId, isActive);
    }

    /** 获取用户安装的RAG详情
     * 
     * @param ragVersionId RAG版本ID
     * @param userId 用户ID
     * @return 安装的RAG详情 */
    public UserRagDTO getInstalledRagDetail(String ragVersionId, String userId) {
        UserRagEntity userRag = userRagDomainService.getInstalledRag(userId, ragVersionId);

        // 转换为DTO并丰富信息
        UserRagDTO dto = UserRagAssembler.toDTO(userRag);
        enrichWithVersionInfo(dto);

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
}