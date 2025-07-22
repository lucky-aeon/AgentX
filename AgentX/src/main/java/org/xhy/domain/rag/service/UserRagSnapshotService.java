package org.xhy.domain.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.rag.model.RagVersionDocumentEntity;
import org.xhy.domain.rag.model.RagVersionFileEntity;
import org.xhy.domain.rag.model.UserRagDocumentEntity;
import org.xhy.domain.rag.model.UserRagFileEntity;
import org.xhy.domain.rag.repository.RagVersionDocumentRepository;
import org.xhy.domain.rag.repository.RagVersionFileRepository;
import org.xhy.domain.rag.repository.UserRagDocumentRepository;
import org.xhy.domain.rag.repository.UserRagFileRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 用户RAG快照服务 - 负责SNAPSHOT类型RAG的数据复制和管理
 * @author xhy
 * @date 2025-07-22 <br/>
 */
@Service
public class UserRagSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(UserRagSnapshotService.class);

    private final UserRagFileRepository userRagFileRepository;
    private final UserRagDocumentRepository userRagDocumentRepository;
    private final RagVersionFileRepository ragVersionFileRepository;
    private final RagVersionDocumentRepository ragVersionDocumentRepository;

    public UserRagSnapshotService(UserRagFileRepository userRagFileRepository,
            UserRagDocumentRepository userRagDocumentRepository, RagVersionFileRepository ragVersionFileRepository,
            RagVersionDocumentRepository ragVersionDocumentRepository) {
        this.userRagFileRepository = userRagFileRepository;
        this.userRagDocumentRepository = userRagDocumentRepository;
        this.ragVersionFileRepository = ragVersionFileRepository;
        this.ragVersionDocumentRepository = ragVersionDocumentRepository;
    }

    /** 为用户安装创建完整快照
     * 
     * @param userRagId 用户RAG安装记录ID
     * @param ragVersionId RAG版本ID */
    @Transactional
    public void createUserSnapshot(String userRagId, String ragVersionId) {
        logger.info("开始为用户RAG [{}] 创建版本 [{}] 的完整快照", userRagId, ragVersionId);

        try {
            // 复制文件快照
            copyVersionFilesToUser(userRagId, ragVersionId);

            // 复制文档快照
            copyVersionDocumentsToUser(userRagId, ragVersionId);

            // TODO: 复制向量数据到用户命名空间
            // copyVectorDataToUser(userRagId, ragVersionId);

            logger.info("用户RAG [{}] 快照创建完成", userRagId);
        } catch (Exception e) {
            logger.error("用户RAG [{}] 快照创建失败", userRagId, e);
            // 回滚已创建的快照数据
            rollbackUserSnapshot(userRagId);
            throw new BusinessException("快照创建失败: " + e.getMessage());
        }
    }

    /** 复制版本文件快照到用户快照
     * 
     * @param userRagId 用户RAG安装记录ID
     * @param ragVersionId RAG版本ID */
    @Transactional
    public void copyVersionFilesToUser(String userRagId, String ragVersionId) {
        // 查询版本文件快照
        LambdaQueryWrapper<RagVersionFileEntity> wrapper = Wrappers.<RagVersionFileEntity>lambdaQuery()
                .eq(RagVersionFileEntity::getRagVersionId, ragVersionId)
                .orderByDesc(RagVersionFileEntity::getCreatedAt);

        List<RagVersionFileEntity> versionFiles = ragVersionFileRepository.selectList(wrapper);

        if (versionFiles.isEmpty()) {
            logger.warn("版本 [{}] 没有找到文件快照", ragVersionId);
            return;
        }

        logger.info("开始复制 {} 个文件快照到用户RAG [{}]", versionFiles.size(), userRagId);

        // 转换并保存为用户文件快照
        for (RagVersionFileEntity versionFile : versionFiles) {
            UserRagFileEntity userFile = convertToUserRagFile(versionFile, userRagId);
            userRagFileRepository.insert(userFile);
        }

        logger.info("文件快照复制完成，共复制 {} 个文件", versionFiles.size());
    }

    /** 复制版本文档快照到用户快照
     * 
     * @param userRagId 用户RAG安装记录ID
     * @param ragVersionId RAG版本ID */
    @Transactional
    public void copyVersionDocumentsToUser(String userRagId, String ragVersionId) {
        // 查询版本文档快照
        LambdaQueryWrapper<RagVersionDocumentEntity> wrapper = Wrappers.<RagVersionDocumentEntity>lambdaQuery()
                .eq(RagVersionDocumentEntity::getRagVersionId, ragVersionId)
                .orderByDesc(RagVersionDocumentEntity::getCreatedAt);

        List<RagVersionDocumentEntity> versionDocuments = ragVersionDocumentRepository.selectList(wrapper);

        if (versionDocuments.isEmpty()) {
            logger.warn("版本 [{}] 没有找到文档快照", ragVersionId);
            return;
        }

        logger.info("开始复制 {} 个文档快照到用户RAG [{}]", versionDocuments.size(), userRagId);

        // 建立文件ID映射关系：版本文件ID -> 用户文件ID
        Map<String, String> fileIdMapping = buildFileIdMapping(userRagId, ragVersionId);

        // 转换并保存为用户文档快照
        for (RagVersionDocumentEntity versionDoc : versionDocuments) {
            UserRagDocumentEntity userDoc = convertToUserRagDocument(versionDoc, userRagId, fileIdMapping);
            userRagDocumentRepository.insert(userDoc);
        }

        logger.info("文档快照复制完成，共复制 {} 个文档", versionDocuments.size());
    }

    /** 删除用户RAG的所有快照数据
     * 
     * @param userRagId 用户RAG安装记录ID */
    @Transactional
    public void deleteUserSnapshot(String userRagId) {
        logger.info("开始删除用户RAG [{}] 的所有快照数据", userRagId);

        // 删除文档快照
        LambdaUpdateWrapper<UserRagDocumentEntity> docDeleteWrapper = Wrappers.<UserRagDocumentEntity>lambdaUpdate()
                .eq(UserRagDocumentEntity::getUserRagId, userRagId);
        userRagDocumentRepository.delete(docDeleteWrapper);

        // 删除文件快照
        LambdaUpdateWrapper<UserRagFileEntity> fileDeleteWrapper = Wrappers.<UserRagFileEntity>lambdaUpdate()
                .eq(UserRagFileEntity::getUserRagId, userRagId);
        userRagFileRepository.delete(fileDeleteWrapper);

        // TODO: 删除向量数据库中的用户命名空间数据
        // deleteUserVectorData(userRagId);

        logger.info("用户RAG [{}] 快照数据删除完成", userRagId);
    }

    /** 获取用户RAG的文件数量
     * 
     * @param userRagId 用户RAG安装记录ID
     * @return 文件数量 */
    public Integer getUserRagFileCount(String userRagId) {
        LambdaQueryWrapper<UserRagFileEntity> wrapper = Wrappers.<UserRagFileEntity>lambdaQuery()
                .eq(UserRagFileEntity::getUserRagId, userRagId);
        return Math.toIntExact(userRagFileRepository.selectCount(wrapper));
    }

    /** 获取用户RAG的文档数量
     * 
     * @param userRagId 用户RAG安装记录ID
     * @return 文档数量 */
    public Integer getUserRagDocumentCount(String userRagId) {
        LambdaQueryWrapper<UserRagDocumentEntity> wrapper = Wrappers.<UserRagDocumentEntity>lambdaQuery()
                .eq(UserRagDocumentEntity::getUserRagId, userRagId);
        return Math.toIntExact(userRagDocumentRepository.selectCount(wrapper));
    }

    // ========== 私有辅助方法 ==========

    /** 回滚用户快照数据 */
    private void rollbackUserSnapshot(String userRagId) {
        try {
            deleteUserSnapshot(userRagId);
        } catch (Exception e) {
            logger.error("回滚用户RAG [{}] 快照数据失败", userRagId, e);
        }
    }

    /** 转换版本文件为用户文件 */
    private UserRagFileEntity convertToUserRagFile(RagVersionFileEntity versionFile, String userRagId) {
        UserRagFileEntity userFile = new UserRagFileEntity();
        userFile.setUserRagId(userRagId);
        userFile.setOriginalFileId(versionFile.getOriginalFileId());
        userFile.setFileName(versionFile.getFileName());
        userFile.setFileSize(versionFile.getFileSize());
        userFile.setFilePageSize(versionFile.getFilePageSize());
        userFile.setFileType(versionFile.getFileType());
        userFile.setFilePath(versionFile.getFilePath());
        userFile.setProcessStatus(versionFile.getProcessStatus());
        userFile.setEmbeddingStatus(versionFile.getEmbeddingStatus());
        return userFile;
    }

    /** 转换版本文档为用户文档 */
    private UserRagDocumentEntity convertToUserRagDocument(RagVersionDocumentEntity versionDoc, String userRagId,
            Map<String, String> fileIdMapping) {
        UserRagDocumentEntity userDoc = new UserRagDocumentEntity();
        userDoc.setUserRagId(userRagId);

        // 根据映射关系设置用户文件ID
        String userRagFileId = fileIdMapping.get(versionDoc.getRagVersionFileId());
        if (userRagFileId == null) {
            logger.warn("未找到版本文件 [{}] 对应的用户文件ID", versionDoc.getRagVersionFileId());
        }
        userDoc.setUserRagFileId(userRagFileId);

        userDoc.setOriginalDocumentId(versionDoc.getOriginalDocumentId());
        userDoc.setContent(versionDoc.getContent());
        userDoc.setPage(versionDoc.getPage());
        userDoc.setVectorId(versionDoc.getVectorId());

        return userDoc;
    }

    /** 建立文件ID映射关系：版本文件ID -> 用户文件ID */
    private Map<String, String> buildFileIdMapping(String userRagId, String ragVersionId) {
        // 获取版本文件列表
        LambdaQueryWrapper<RagVersionFileEntity> versionWrapper = Wrappers.<RagVersionFileEntity>lambdaQuery()
                .eq(RagVersionFileEntity::getRagVersionId, ragVersionId)
                .orderByDesc(RagVersionFileEntity::getCreatedAt);
        List<RagVersionFileEntity> versionFiles = ragVersionFileRepository.selectList(versionWrapper);

        // 获取对应的用户文件列表
        LambdaQueryWrapper<UserRagFileEntity> userWrapper = Wrappers.<UserRagFileEntity>lambdaQuery()
                .eq(UserRagFileEntity::getUserRagId, userRagId).orderByDesc(UserRagFileEntity::getCreatedAt);
        List<UserRagFileEntity> userFiles = userRagFileRepository.selectList(userWrapper);

        // 建立映射关系（基于originalFileId）
        Map<String, String> fileIdMapping = new HashMap<>();
        Map<String, String> originalToUserMap = new HashMap<>();

        // 先建立原始文件ID到用户文件ID的映射
        for (UserRagFileEntity userFile : userFiles) {
            originalToUserMap.put(userFile.getOriginalFileId(), userFile.getId());
        }

        // 再建立版本文件ID到用户文件ID的映射
        for (RagVersionFileEntity versionFile : versionFiles) {
            String userFileId = originalToUserMap.get(versionFile.getOriginalFileId());
            if (userFileId != null) {
                fileIdMapping.put(versionFile.getId(), userFileId);
            }
        }

        return fileIdMapping;
    }
}