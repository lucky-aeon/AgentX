package org.xhy.application.rag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.rag.assembler.RagVersionAssembler;
import org.xhy.application.rag.dto.RagVersionDTO;
import org.xhy.application.rag.request.PublishRagRequest;
import org.xhy.application.rag.request.ReviewRagVersionRequest;
import org.xhy.domain.rag.constant.RagPublishStatus;
import org.xhy.domain.rag.model.RagVersionEntity;
import org.xhy.domain.rag.service.RagVersionDomainService;
import org.xhy.domain.rag.service.UserRagDomainService;
import org.xhy.domain.user.service.UserDomainService;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;

/** RAG发布应用服务
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@Service
public class RagPublishAppService {

    private final RagVersionDomainService ragVersionDomainService;
    private final UserRagDomainService userRagDomainService;
    private final UserDomainService userDomainService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagPublishAppService(RagVersionDomainService ragVersionDomainService,
                                UserRagDomainService userRagDomainService,
                                UserDomainService userDomainService) {
        this.ragVersionDomainService = ragVersionDomainService;
        this.userRagDomainService = userRagDomainService;
        this.userDomainService = userDomainService;
    }

    /**
     * 发布RAG版本
     * 
     * @param request 发布请求
     * @param userId 用户ID
     * @return 发布的版本信息
     */
    @Transactional
    public RagVersionDTO publishRagVersion(PublishRagRequest request, String userId) {
        // 处理标签JSON
        String labelsJson = null;
        if (request.getLabels() != null && !request.getLabels().isEmpty()) {
            try {
                labelsJson = objectMapper.writeValueAsString(request.getLabels());
            } catch (Exception e) {
                throw new BusinessException("标签格式错误");
            }
        }
        
        // 创建版本快照
        RagVersionEntity ragVersion = ragVersionDomainService.createRagVersionSnapshot(
                request.getRagId(),
                request.getVersion(),
                request.getChangeLog(),
                userId
        );
        
        // 设置标签
        if (labelsJson != null) {
            ragVersion.setLabels(labelsJson);
            ragVersionDomainService.getRagVersion(ragVersion.getId()); // 触发更新
        }
        
        // 转换为DTO
        RagVersionDTO dto = RagVersionAssembler.toDTO(ragVersion);
        
        // 设置用户信息
        enrichWithUserInfo(dto);
        
        return dto;
    }

    /**
     * 获取用户的RAG版本列表
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @return 版本列表
     */
    public Page<RagVersionDTO> getUserRagVersions(String userId, Integer page, Integer pageSize, String keyword) {
        IPage<RagVersionEntity> entityPage = ragVersionDomainService.listUserVersions(userId, page, pageSize, keyword);
        
        // 转换为DTO
        List<RagVersionDTO> dtoList = RagVersionAssembler.toDTOs(entityPage.getRecords());
        
        // 设置用户信息和安装次数
        for (RagVersionDTO dto : dtoList) {
            enrichWithUserInfo(dto);
            dto.setInstallCount(userRagDomainService.getInstallCount(dto.getId()));
        }
        
        // 创建DTO分页对象
        Page<RagVersionDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }

    /**
     * 获取RAG的版本历史
     * 
     * @param ragId 原始RAG数据集ID
     * @param userId 用户ID
     * @return 版本历史列表
     */
    public List<RagVersionDTO> getRagVersionHistory(String ragId, String userId) {
        List<RagVersionEntity> versions = ragVersionDomainService.getVersionHistory(ragId, userId);
        
        // 转换为DTO
        List<RagVersionDTO> dtoList = RagVersionAssembler.toDTOs(versions);
        
        // 设置用户信息
        for (RagVersionDTO dto : dtoList) {
            enrichWithUserInfo(dto);
        }
        
        return dtoList;
    }

    /**
     * 获取RAG版本详情
     * 
     * @param versionId 版本ID
     * @param currentUserId 当前用户ID（用于判断是否已安装）
     * @return 版本详情
     */
    public RagVersionDTO getRagVersionDetail(String versionId, String currentUserId) {
        RagVersionEntity version = ragVersionDomainService.getRagVersion(versionId);
        
        // 转换为DTO
        RagVersionDTO dto = RagVersionAssembler.toDTO(version);
        
        // 设置用户信息
        enrichWithUserInfo(dto);
        
        // 设置安装次数
        dto.setInstallCount(userRagDomainService.getInstallCount(versionId));
        
        // 设置是否已安装
        if (StringUtils.isNotBlank(currentUserId)) {
            dto.setIsInstalled(userRagDomainService.isRagInstalled(currentUserId, versionId));
        }
        
        return dto;
    }

    /**
     * 管理员审核RAG版本
     * 
     * @param versionId 版本ID
     * @param request 审核请求
     * @return 审核后的版本信息
     */
    @Transactional
    public RagVersionDTO reviewRagVersion(String versionId, ReviewRagVersionRequest request) {
        // 获取审核状态
        RagPublishStatus status = RagPublishStatus.fromCode(request.getStatus());
        if (status == null || (status != RagPublishStatus.PUBLISHED && status != RagPublishStatus.REJECTED)) {
            throw new BusinessException("无效的审核状态");
        }
        
        // 如果是拒绝，检查拒绝原因
        if (status == RagPublishStatus.REJECTED && StringUtils.isBlank(request.getRejectReason())) {
            throw new BusinessException("拒绝时必须填写拒绝原因");
        }
        
        // 更新审核状态
        ragVersionDomainService.updateReviewStatus(versionId, status, request.getRejectReason());
        
        // 返回更新后的版本信息
        return getRagVersionDetail(versionId, null);
    }

    /**
     * 获取待审核的RAG版本列表
     * 
     * @param page 页码
     * @param pageSize 每页大小
     * @return 待审核版本列表
     */
    public Page<RagVersionDTO> getPendingReviewVersions(Integer page, Integer pageSize) {
        IPage<RagVersionEntity> entityPage = ragVersionDomainService.listPendingReviewVersions(page, pageSize);
        
        // 转换为DTO
        List<RagVersionDTO> dtoList = RagVersionAssembler.toDTOs(entityPage.getRecords());
        
        // 设置用户信息
        for (RagVersionDTO dto : dtoList) {
            enrichWithUserInfo(dto);
        }
        
        // 创建DTO分页对象
        Page<RagVersionDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }

    /**
     * 下架RAG版本
     * 
     * @param versionId 版本ID
     * @return 下架后的版本信息
     */
    @Transactional
    public RagVersionDTO removeRagVersion(String versionId) {
        // 更新状态为已下架
        ragVersionDomainService.updateReviewStatus(versionId, RagPublishStatus.REMOVED, null);
        
        // 返回更新后的版本信息
        return getRagVersionDetail(versionId, null);
    }

    /**
     * 丰富用户信息
     * 
     * @param dto RAG版本DTO
     */
    private void enrichWithUserInfo(RagVersionDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getUserId())) {
            return;
        }
        
        try {
            var user = userDomainService.getUserInfo(dto.getUserId());
            if (user != null) {
                dto.setUserNickname(user.getNickname());
            }
        } catch (Exception e) {
            // 忽略用户查询异常
        }
    }
}