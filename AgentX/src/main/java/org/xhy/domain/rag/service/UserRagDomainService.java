package org.xhy.domain.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.rag.constant.InstallType;
import org.xhy.domain.rag.constant.RagPublishStatus;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
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
    private final RagQaDatasetDomainService ragQaDatasetDomainService;

    public UserRagDomainService(UserRagRepository userRagRepository, RagVersionDomainService ragVersionDomainService,
            RagQaDatasetDomainService ragQaDatasetDomainService) {
        this.userRagRepository = userRagRepository;
        this.ragVersionDomainService = ragVersionDomainService;
        this.ragQaDatasetDomainService = ragQaDatasetDomainService;
    }

    /** 安装RAG（新版本 - 安装RAG本身而不是特定版本）
     * 
     * @param userId 用户ID
     * @param ragVersionId RAG版本ID
     * @return 安装记录 */
    @Transactional
    public UserRagEntity installRag(String userId, String ragVersionId) {
        // 验证版本存在
        RagVersionEntity ragVersion = ragVersionDomainService.getRagVersion(ragVersionId);

        // 检查0.0.1版本权限：只有创建者可以安装0.0.1版本
        if ("0.0.1".equals(ragVersion.getVersion()) && !ragVersion.getUserId().equals(userId)) {
            throw new BusinessException("0.0.1版本为作者私有版本，不支持安装");
        }

        // 如果是自己的RAG，允许安装任何状态的版本（包括私有版本）
        // 如果是他人的RAG，只能安装已发布的版本
        if (!ragVersion.getUserId().equals(userId)
                && !RagPublishStatus.PUBLISHED.getCode().equals(ragVersion.getPublishStatus())) {
            throw new BusinessException("该RAG版本未发布或已下架");
        }

        // 检查是否已安装同一个原始RAG
        UserRagEntity existingRag = findInstalledRagByOriginalId(userId, ragVersion.getOriginalRagId());
        if (existingRag != null) {
            // 如果已安装，则切换到新版本
            return switchRagVersion(userId, existingRag.getId(), ragVersionId);
        }

        // 确定安装类型
        InstallType installType = determineInstallType(userId, ragVersion);

        // 创建安装记录（完整快照）
        UserRagEntity userRag = new UserRagEntity();
        userRag.setUserId(userId);
        userRag.setRagVersionId(ragVersionId);
        userRag.setOriginalRagId(ragVersion.getOriginalRagId());
        userRag.setInstallType(installType);
        userRag.setName(ragVersion.getName());
        userRag.setDescription(ragVersion.getDescription());
        userRag.setIcon(ragVersion.getIcon());
        userRag.setVersion(ragVersion.getVersion());
        userRag.setInstalledAt(LocalDateTime.now());

        userRagRepository.insert(userRag);
        return userRag;
    }

    /** 自动安装RAG（用于创建数据集时）
     * 
     * @param userId 用户ID
     * @param originalRagId 原始RAG数据集ID
     * @param ragVersionId RAG版本ID
     * @return 安装记录 */
    @Transactional
    public UserRagEntity autoInstallRag(String userId, String originalRagId, String ragVersionId) {
        // 检查是否已安装
        UserRagEntity existingRag = findInstalledRagByOriginalId(userId, originalRagId);
        if (existingRag != null) {
            return existingRag;
        }

        // 获取RAG和版本信息
        RagQaDatasetEntity ragDataset = ragQaDatasetDomainService.getDataset(originalRagId, userId);
        RagVersionEntity ragVersion = ragVersionDomainService.getRagVersion(ragVersionId);

        // 创建REFERENCE类型的安装记录（自己创建的RAG）
        UserRagEntity userRag = new UserRagEntity();
        userRag.setUserId(userId);
        userRag.setRagVersionId(ragVersionId);
        userRag.setOriginalRagId(originalRagId);
        userRag.setInstallType(InstallType.REFERENCE); // 自动安装的都是引用类型
        userRag.setName(ragDataset.getName());
        userRag.setDescription(ragDataset.getDescription());
        userRag.setIcon(ragDataset.getIcon());
        userRag.setVersion(ragVersion.getVersion());
        userRag.setInstalledAt(LocalDateTime.now());

        userRagRepository.insert(userRag);
        return userRag;
    }

    /** 切换RAG版本
     * 
     * @param userId 用户ID
     * @param userRagId 用户RAG安装记录ID
     * @param targetVersionId 目标版本ID
     * @return 更新后的安装记录 */
    @Transactional
    public UserRagEntity switchRagVersion(String userId, String userRagId, String targetVersionId) {
        // 获取当前安装记录
        UserRagEntity userRag = getUserRag(userId, userRagId);

        // 验证目标版本
        RagVersionEntity targetVersion = ragVersionDomainService.getRagVersion(targetVersionId);

        // 检查0.0.1版本权限：只有创建者可以切换到0.0.1版本
        if ("0.0.1".equals(targetVersion.getVersion()) && !targetVersion.getUserId().equals(userId)) {
            throw new BusinessException("0.0.1版本为作者私有版本，不支持切换");
        }

        // 权限检查：确保目标版本属于同一个原始RAG
        if (!targetVersion.getOriginalRagId().equals(userRag.getOriginalRagId())) {
            throw new BusinessException("目标版本不属于当前RAG");
        }

        // 权限检查：如果不是创建者，只能切换到已发布版本
        if (!targetVersion.getUserId().equals(userId)
                && !RagPublishStatus.PUBLISHED.getCode().equals(targetVersion.getPublishStatus())) {
            throw new BusinessException("该版本未发布，无法切换");
        }

        // 确定新的安装类型
        InstallType newInstallType = determineInstallType(userId, targetVersion);

        // 更新安装记录（更新快照数据）
        LambdaUpdateWrapper<UserRagEntity> updateWrapper = Wrappers.<UserRagEntity>lambdaUpdate()
                .eq(UserRagEntity::getId, userRagId).eq(UserRagEntity::getUserId, userId)
                .set(UserRagEntity::getRagVersionId, targetVersionId).set(UserRagEntity::getInstallType, newInstallType)
                .set(UserRagEntity::getVersion, targetVersion.getVersion())
                .set(UserRagEntity::getName, targetVersion.getName())
                .set(UserRagEntity::getDescription, targetVersion.getDescription())
                .set(UserRagEntity::getIcon, targetVersion.getIcon());

        userRagRepository.checkedUpdate(null, updateWrapper);

        // 返回更新后的记录
        return getUserRag(userId, userRagId);
    }

    /** 卸载RAG版本
     * 
     * @param userId 用户ID
     * @param ragVersionId RAG版本ID */
    public void uninstallRag(String userId, String ragVersionId) {
        // 检查是否为用户自己的知识库
        try {
            RagVersionEntity ragVersion = ragVersionDomainService.getRagVersion(ragVersionId);

            // 如果是用户自己创建的知识库且源知识库还存在，则不允许卸载
            if (ragVersion.getUserId().equals(userId)) {
                throw new BusinessException("无法卸载自己创建的知识库，请先删除原知识库");
            }
        } catch (BusinessException e) {
            // 如果是"无法卸载自己创建的知识库"异常，则重新抛出
            if (e.getMessage().contains("无法卸载自己创建的知识库")) {
                throw e;
            }
            // 如果是其他异常（比如源知识库不存在），则继续执行卸载
            // 这种情况下，源知识库已被删除，允许卸载残留的安装记录
        }

        LambdaUpdateWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaUpdate()
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getRagVersionId, ragVersionId);

        userRagRepository.delete(wrapper);
    }

    /** 检查RAG版本是否已安装（兼容性方法）
     * 
     * @param userId 用户ID
     * @param ragVersionId RAG版本ID
     * @return 是否已安装 */
    public boolean isRagInstalled(String userId, String ragVersionId) {
        LambdaQueryWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaQuery()
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getRagVersionId, ragVersionId);

        return userRagRepository.exists(wrapper);
    }

    /** 检查RAG是否已安装（按原始RAG ID检查）
     * 
     * @param userId 用户ID
     * @param originalRagId 原始RAG数据集ID
     * @return 是否已安装 */
    public boolean isRagInstalledByOriginalId(String userId, String originalRagId) {
        return findInstalledRagByOriginalId(userId, originalRagId) != null;
    }

    /** 查找用户安装的RAG（按原始RAG ID）
     * 
     * @param userId 用户ID
     * @param originalRagId 原始RAG数据集ID
     * @return 安装记录，如果未安装则返回null */
    public UserRagEntity findInstalledRagByOriginalId(String userId, String originalRagId) {
        LambdaQueryWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaQuery()
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getOriginalRagId, originalRagId);

        return userRagRepository.selectOne(wrapper);
    }

    /** 获取用户安装的RAG列表
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @return 分页结果 */
    public IPage<UserRagEntity> listInstalledRags(String userId, Integer page, Integer pageSize, String keyword) {
        LambdaQueryWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaQuery().eq(UserRagEntity::getUserId,
                userId);

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
                .eq(UserRagEntity::getUserId, userId).orderByDesc(UserRagEntity::getInstalledAt);

        return userRagRepository.selectList(wrapper);
    }

    /** 获取用户安装的RAG详情（兼容性方法）
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

    /** 获取用户RAG详情（按安装记录ID）
     * 
     * @param userId 用户ID
     * @param userRagId 用户RAG安装记录ID
     * @return 安装的RAG */
    public UserRagEntity getUserRag(String userId, String userRagId) {
        LambdaQueryWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaQuery()
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getId, userRagId);

        UserRagEntity userRag = userRagRepository.selectOne(wrapper);
        if (userRag == null) {
            throw new BusinessException("未找到该RAG安装记录");
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

    /** 获取RAG版本的安装次数
     * 
     * @param ragVersionId RAG版本ID
     * @return 安装次数 */
    public long getInstallCount(String ragVersionId) {
        LambdaQueryWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaQuery()
                .eq(UserRagEntity::getRagVersionId, ragVersionId);

        return userRagRepository.selectCount(wrapper);
    }

    /** 获取原始RAG的安装次数
     * 
     * @param originalRagId 原始RAG数据集ID
     * @return 安装次数 */
    public long getInstallCountByOriginalId(String originalRagId) {
        LambdaQueryWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaQuery()
                .eq(UserRagEntity::getOriginalRagId, originalRagId);

        return userRagRepository.selectCount(wrapper);
    }

    /** 强制卸载RAG版本（用于数据集删除时清理，不进行业务校验）
     * 
     * @param userId 用户ID
     * @param ragVersionId RAG版本ID */
    public void forceUninstallRag(String userId, String ragVersionId) {
        LambdaUpdateWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaUpdate()
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getRagVersionId, ragVersionId);

        userRagRepository.checkedDelete(wrapper);
    }

    /** 强制卸载RAG（按原始RAG ID，用于数据集删除时清理）
     * 
     * @param userId 用户ID
     * @param originalRagId 原始RAG数据集ID */
    public void forceUninstallRagByOriginalId(String userId, String originalRagId) {
        LambdaUpdateWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaUpdate()
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getOriginalRagId, originalRagId);

        userRagRepository.checkedDelete(wrapper);
    }

    /** 确定安装类型
     * 
     * @param userId 用户ID
     * @param ragVersion RAG版本
     * @return 安装类型 */
    private InstallType determineInstallType(String userId, RagVersionEntity ragVersion) {
        // 如果是自己创建的RAG，使用REFERENCE类型（动态引用）
        if (ragVersion.getUserId().equals(userId)) {
            return InstallType.REFERENCE;
        }
        // 如果是他人的RAG，使用SNAPSHOT类型（版本快照）
        return InstallType.SNAPSHOT;
    }
}