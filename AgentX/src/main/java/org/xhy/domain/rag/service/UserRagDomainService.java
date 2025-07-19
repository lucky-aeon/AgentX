package org.xhy.domain.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.rag.constant.RagPublishStatus;
import org.xhy.domain.rag.model.RagVersionEntity;
import org.xhy.domain.rag.model.UserRagEntity;
import org.xhy.domain.rag.repository.UserRagRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;

/** 用户RAG领域服务
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@Service
public class UserRagDomainService {

    private final UserRagRepository userRagRepository;
    private final RagVersionDomainService ragVersionDomainService;

    public UserRagDomainService(UserRagRepository userRagRepository, RagVersionDomainService ragVersionDomainService) {
        this.userRagRepository = userRagRepository;
        this.ragVersionDomainService = ragVersionDomainService;
    }

    /** 安装RAG版本
     * 
     * @param userId 用户ID
     * @param ragVersionId RAG版本ID
     * @return 安装记录 */
    @Transactional
    public UserRagEntity installRag(String userId, String ragVersionId) {
        // 验证版本存在且已发布
        RagVersionEntity ragVersion = ragVersionDomainService.getRagVersion(ragVersionId);
        if (!RagPublishStatus.PUBLISHED.getCode().equals(ragVersion.getPublishStatus())) {
            throw new BusinessException("该RAG版本未发布或已下架");
        }

        // 检查是否已安装
        if (isRagInstalled(userId, ragVersionId)) {
            throw new BusinessException("该RAG版本已安装");
        }

        // 创建安装记录
        UserRagEntity userRag = new UserRagEntity();
        userRag.setUserId(userId);
        userRag.setRagVersionId(ragVersionId);
        userRag.setName(ragVersion.getName());
        userRag.setDescription(ragVersion.getDescription());
        userRag.setIcon(ragVersion.getIcon());
        userRag.setVersion(ragVersion.getVersion());
        userRag.setIsActive(true);
        userRag.setInstalledAt(LocalDateTime.now());

        userRagRepository.insert(userRag);
        return userRag;
    }

    /** 卸载RAG版本
     * 
     * @param userId 用户ID
     * @param ragVersionId RAG版本ID */
    public void uninstallRag(String userId, String ragVersionId) {
        LambdaUpdateWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaUpdate()
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getRagVersionId, ragVersionId);

        userRagRepository.checkedDelete(wrapper);
    }

    /** 检查RAG是否已安装
     * 
     * @param userId 用户ID
     * @param ragVersionId RAG版本ID
     * @return 是否已安装 */
    public boolean isRagInstalled(String userId, String ragVersionId) {
        LambdaQueryWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaQuery()
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getRagVersionId, ragVersionId);

        return userRagRepository.exists(wrapper);
    }

    /** 获取用户安装的RAG列表
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @return 分页结果 */
    public IPage<UserRagEntity> listInstalledRags(String userId, Integer page, Integer pageSize, String keyword) {
        LambdaQueryWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaQuery()
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getIsActive, true);

        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(UserRagEntity::getName, keyword).or().like(UserRagEntity::getDescription, keyword));
        }

        wrapper.orderByDesc(UserRagEntity::getInstalledAt);

        Page<UserRagEntity> pageObj = new Page<>(page, pageSize);
        return userRagRepository.selectPage(pageObj, wrapper);
    }

    /** 获取用户安装的所有RAG
     * 
     * @param userId 用户ID
     * @return RAG列表 */
    public List<UserRagEntity> listAllInstalledRags(String userId) {
        LambdaQueryWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaQuery()
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getIsActive, true)
                .orderByDesc(UserRagEntity::getInstalledAt);

        return userRagRepository.selectList(wrapper);
    }

    /** 更新安装的RAG状态
     * 
     * @param userId 用户ID
     * @param ragVersionId RAG版本ID
     * @param isActive 是否激活 */
    public void updateRagStatus(String userId, String ragVersionId, boolean isActive) {
        LambdaUpdateWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaUpdate()
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getRagVersionId, ragVersionId)
                .set(UserRagEntity::getIsActive, isActive);

        userRagRepository.checkedUpdate(null, wrapper);
    }

    /** 获取用户安装的RAG详情
     * 
     * @param userId 用户ID
     * @param ragVersionId RAG版本ID
     * @return 安装的RAG */
    public UserRagEntity getInstalledRag(String userId, String ragVersionId) {
        LambdaQueryWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaQuery()
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getRagVersionId, ragVersionId);

        UserRagEntity userRag = userRagRepository.selectOne(wrapper);
        if (userRag == null) {
            throw new BusinessException("未安装该RAG版本");
        }

        return userRag;
    }

    /** 检查用户是否有权限使用RAG
     * 
     * @param userId 用户ID
     * @param ragId 原始RAG数据集ID（可选）
     * @param ragVersionId RAG版本ID（可选）
     * @return 是否有权限 */
    public boolean canUseRag(String userId, String ragId, String ragVersionId) {
        if (StringUtils.isNotBlank(ragVersionId)) {
            // 检查是否已安装该版本
            return isRagInstalled(userId, ragVersionId);
        } else if (StringUtils.isNotBlank(ragId)) {
            // 检查是否为创建者（需要调用其他服务）
            // 这里假设创建者总是有权限
            return true;
        }

        return false;
    }

    /** 获取RAG的安装次数
     * 
     * @param ragVersionId RAG版本ID
     * @return 安装次数 */
    public long getInstallCount(String ragVersionId) {
        LambdaQueryWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaQuery()
                .eq(UserRagEntity::getRagVersionId, ragVersionId);

        return userRagRepository.selectCount(wrapper);
    }
}