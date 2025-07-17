package org.xhy.domain.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.rag.constant.RagPublishStatus;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.model.RagQaDatasetEntity;
import org.xhy.domain.rag.model.RagVersionDocumentEntity;
import org.xhy.domain.rag.model.RagVersionEntity;
import org.xhy.domain.rag.model.RagVersionFileEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.domain.rag.repository.RagVersionDocumentRepository;
import org.xhy.domain.rag.repository.RagVersionFileRepository;
import org.xhy.domain.rag.repository.RagVersionRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;

/** RAG版本领域服务
 * @author xhy
 * @date 2025-07-16 <br/>
 */
@Service
public class RagVersionDomainService {

    private final RagVersionRepository ragVersionRepository;
    private final RagVersionFileRepository ragVersionFileRepository;
    private final RagVersionDocumentRepository ragVersionDocumentRepository;
    private final RagQaDatasetDomainService ragQaDatasetDomainService;
    private final FileDetailRepository fileDetailRepository;
    private final DocumentUnitRepository documentUnitRepository;

    public RagVersionDomainService(RagVersionRepository ragVersionRepository,
                                   RagVersionFileRepository ragVersionFileRepository,
                                   RagVersionDocumentRepository ragVersionDocumentRepository,
                                   RagQaDatasetDomainService ragQaDatasetDomainService,
                                   FileDetailRepository fileDetailRepository,
                                   DocumentUnitRepository documentUnitRepository) {
        this.ragVersionRepository = ragVersionRepository;
        this.ragVersionFileRepository = ragVersionFileRepository;
        this.ragVersionDocumentRepository = ragVersionDocumentRepository;
        this.ragQaDatasetDomainService = ragQaDatasetDomainService;
        this.fileDetailRepository = fileDetailRepository;
        this.documentUnitRepository = documentUnitRepository;
    }

    /** 创建RAG版本快照
     * 
     * @param ragId 原始RAG数据集ID
     * @param version 版本号
     * @param changeLog 更新日志
     * @param userId 用户ID
     * @return 创建的RAG版本
     */
    @Transactional
    public RagVersionEntity createRagVersionSnapshot(String ragId, String version, String changeLog, String userId) {
        // 验证原始数据集存在
        RagQaDatasetEntity dataset = ragQaDatasetDomainService.getDataset(ragId, userId);
        
        // 验证版本号唯一性
        validateVersionUniqueness(ragId, version);
        
        // 创建版本记录
        RagVersionEntity ragVersion = new RagVersionEntity();
        ragVersion.setName(dataset.getName());
        ragVersion.setIcon(dataset.getIcon());
        ragVersion.setDescription(dataset.getDescription());
        ragVersion.setUserId(userId);
        ragVersion.setVersion(version);
        ragVersion.setChangeLog(changeLog);
        ragVersion.setOriginalRagId(ragId);
        ragVersion.setOriginalRagName(dataset.getName());
        ragVersion.setPublishStatus(RagPublishStatus.REVIEWING.getCode());
        ragVersionRepository.insert(ragVersion);
        
        // 复制文件和文档数据
        copyFilesAndDocuments(ragId, ragVersion.getId());
        
        // 更新统计信息
        updateVersionStatistics(ragVersion.getId());
        
        return ragVersion;
    }

    /** 复制文件和文档数据到版本快照
     * 
     * @param ragId 原始RAG数据集ID
     * @param ragVersionId RAG版本ID
     */
    private void copyFilesAndDocuments(String ragId, String ragVersionId) {
        // 获取原始文件列表
        LambdaQueryWrapper<FileDetailEntity> fileWrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getDataSetId, ragId);
        List<FileDetailEntity> originalFiles = fileDetailRepository.selectList(fileWrapper);
        
        for (FileDetailEntity originalFile : originalFiles) {
            // 创建文件快照
            RagVersionFileEntity versionFile = new RagVersionFileEntity();
            versionFile.setRagVersionId(ragVersionId);
            versionFile.setOriginalFileId(originalFile.getId());
            versionFile.setFileName(originalFile.getOriginalFilename());
            versionFile.setFileSize(originalFile.getSize());
            versionFile.setFileType(originalFile.getExt());
            versionFile.setFilePath(originalFile.getPath());
            versionFile.setProcessStatus(originalFile.getIsInitialize());
            versionFile.setEmbeddingStatus(originalFile.getIsEmbedding());
            ragVersionFileRepository.insert(versionFile);
            
            // 复制文档单元
            copyDocumentUnits(originalFile.getId(), ragVersionId, versionFile.getId());
        }
    }

    /** 复制文档单元到版本快照
     * 
     * @param originalFileId 原始文件ID
     * @param ragVersionId RAG版本ID
     * @param ragVersionFileId RAG版本文件ID
     */
    private void copyDocumentUnits(String originalFileId, String ragVersionId, String ragVersionFileId) {
        LambdaQueryWrapper<DocumentUnitEntity> docWrapper = Wrappers.<DocumentUnitEntity>lambdaQuery()
                .eq(DocumentUnitEntity::getFileId, originalFileId);
        List<DocumentUnitEntity> documents = documentUnitRepository.selectList(docWrapper);
        
        for (DocumentUnitEntity doc : documents) {
            RagVersionDocumentEntity versionDoc = new RagVersionDocumentEntity();
            versionDoc.setRagVersionId(ragVersionId);
            versionDoc.setRagVersionFileId(ragVersionFileId);
            versionDoc.setOriginalDocumentId(doc.getId());
            versionDoc.setContent(doc.getContent());
            versionDoc.setPage(doc.getPage());
            // vectorId 将在向量复制时设置
            ragVersionDocumentRepository.insert(versionDoc);
        }
    }

    /** 更新版本统计信息
     * 
     * @param ragVersionId RAG版本ID
     */
    private void updateVersionStatistics(String ragVersionId) {
        // 统计文件数量和大小
        LambdaQueryWrapper<RagVersionFileEntity> fileWrapper = Wrappers.<RagVersionFileEntity>lambdaQuery()
                .eq(RagVersionFileEntity::getRagVersionId, ragVersionId);
        List<RagVersionFileEntity> files = ragVersionFileRepository.selectList(fileWrapper);
        
        int fileCount = files.size();
        long totalSize = files.stream().mapToLong(RagVersionFileEntity::getFileSize).sum();
        
        // 统计文档数量
        LambdaQueryWrapper<RagVersionDocumentEntity> docWrapper = Wrappers.<RagVersionDocumentEntity>lambdaQuery()
                .eq(RagVersionDocumentEntity::getRagVersionId, ragVersionId);
        long documentCount = ragVersionDocumentRepository.selectCount(docWrapper);
        
        // 更新版本记录
        RagVersionEntity update = new RagVersionEntity();
        update.setId(ragVersionId);
        update.setFileCount(fileCount);
        update.setTotalSize(totalSize);
        update.setDocumentCount((int) documentCount);
        ragVersionRepository.updateById(update);
    }

    /** 验证版本号唯一性
     * 
     * @param ragId 原始RAG数据集ID
     * @param version 版本号
     */
    private void validateVersionUniqueness(String ragId, String version) {
        LambdaQueryWrapper<RagVersionEntity> wrapper = Wrappers.<RagVersionEntity>lambdaQuery()
                .eq(RagVersionEntity::getOriginalRagId, ragId)
                .eq(RagVersionEntity::getVersion, version);
        if (ragVersionRepository.exists(wrapper)) {
            throw new BusinessException("版本号已存在");
        }
    }

    /** 获取RAG版本详情
     * 
     * @param versionId 版本ID
     * @return RAG版本实体
     */
    public RagVersionEntity getRagVersion(String versionId) {
        RagVersionEntity version = ragVersionRepository.selectById(versionId);
        if (version == null) {
            throw new BusinessException("RAG版本不存在");
        }
        return version;
    }

    /** 更新审核状态
     * 
     * @param versionId 版本ID
     * @param status 审核状态
     * @param rejectReason 拒绝原因（可选）
     */
    public void updateReviewStatus(String versionId, RagPublishStatus status, String rejectReason) {
        RagVersionEntity update = new RagVersionEntity();
        update.setId(versionId);
        update.setPublishStatus(status.getCode());
        update.setReviewTime(LocalDateTime.now());
        
        if (status == RagPublishStatus.PUBLISHED) {
            update.setPublishedAt(LocalDateTime.now());
        } else if (status == RagPublishStatus.REJECTED) {
            update.setRejectReason(rejectReason);
        }
        
        ragVersionRepository.updateById(update);
    }

    /** 分页查询用户的RAG版本
     * 
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @return 分页结果
     */
    public IPage<RagVersionEntity> listUserVersions(String userId, Integer page, Integer pageSize, String keyword) {
        LambdaQueryWrapper<RagVersionEntity> wrapper = Wrappers.<RagVersionEntity>lambdaQuery()
                .eq(RagVersionEntity::getUserId, userId);
        
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(RagVersionEntity::getName, keyword)
                    .or().like(RagVersionEntity::getDescription, keyword));
        }
        
        wrapper.orderByDesc(RagVersionEntity::getCreatedAt);
        
        Page<RagVersionEntity> pageObj = new Page<>(page, pageSize);
        return ragVersionRepository.selectPage(pageObj, wrapper);
    }

    /** 分页查询已发布的RAG版本（市场）
     * 
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @return 分页结果
     */
    public IPage<RagVersionEntity> listPublishedVersions(Integer page, Integer pageSize, String keyword) {
        LambdaQueryWrapper<RagVersionEntity> wrapper = Wrappers.<RagVersionEntity>lambdaQuery()
                .eq(RagVersionEntity::getPublishStatus, RagPublishStatus.PUBLISHED.getCode());
        
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(RagVersionEntity::getName, keyword)
                    .or().like(RagVersionEntity::getDescription, keyword));
        }
        
        wrapper.orderByDesc(RagVersionEntity::getPublishedAt);
        
        Page<RagVersionEntity> pageObj = new Page<>(page, pageSize);
        return ragVersionRepository.selectPage(pageObj, wrapper);
    }

    /** 获取待审核的RAG版本列表
     * 
     * @param page 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public IPage<RagVersionEntity> listPendingReviewVersions(Integer page, Integer pageSize) {
        LambdaQueryWrapper<RagVersionEntity> wrapper = Wrappers.<RagVersionEntity>lambdaQuery()
                .eq(RagVersionEntity::getPublishStatus, RagPublishStatus.REVIEWING.getCode())
                .orderByAsc(RagVersionEntity::getCreatedAt);
        
        Page<RagVersionEntity> pageObj = new Page<>(page, pageSize);
        return ragVersionRepository.selectPage(pageObj, wrapper);
    }

    /** 获取RAG的版本历史
     * 
     * @param ragId 原始RAG数据集ID
     * @param userId 用户ID
     * @return 版本列表
     */
    public List<RagVersionEntity> getVersionHistory(String ragId, String userId) {
        LambdaQueryWrapper<RagVersionEntity> wrapper = Wrappers.<RagVersionEntity>lambdaQuery()
                .eq(RagVersionEntity::getOriginalRagId, ragId)
                .eq(RagVersionEntity::getUserId, userId)
                .orderByDesc(RagVersionEntity::getCreatedAt);
        
        return ragVersionRepository.selectList(wrapper);
    }
}