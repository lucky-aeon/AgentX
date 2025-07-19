package org.xhy.domain.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.constant.InstallType;
import org.xhy.domain.rag.model.*;
import org.xhy.domain.rag.repository.*;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;

/** RAG数据访问服务 - 支持动态引用和快照数据获取
 * @author xhy
 * @date 2025-07-19 <br/>
 */
@Service
public class RagDataAccessService {

    private final UserRagRepository userRagRepository;
    private final FileDetailRepository fileDetailRepository;
    private final DocumentUnitRepository documentUnitRepository;
    private final RagVersionFileRepository ragVersionFileRepository;
    private final RagVersionDocumentRepository ragVersionDocumentRepository;

    public RagDataAccessService(UserRagRepository userRagRepository,
                               FileDetailRepository fileDetailRepository,
                               DocumentUnitRepository documentUnitRepository,
                               RagVersionFileRepository ragVersionFileRepository,
                               RagVersionDocumentRepository ragVersionDocumentRepository) {
        this.userRagRepository = userRagRepository;
        this.fileDetailRepository = fileDetailRepository;
        this.documentUnitRepository = documentUnitRepository;
        this.ragVersionFileRepository = ragVersionFileRepository;
        this.ragVersionDocumentRepository = ragVersionDocumentRepository;
    }

    /** 获取用户可用的RAG文件列表
     * 
     * @param userId 用户ID
     * @param userRagId 用户RAG安装记录ID
     * @return 文件列表 */
    public List<FileDetailEntity> getRagFiles(String userId, String userRagId) {
        UserRagEntity userRag = getUserRag(userId, userRagId);
        
        if (userRag.isReferenceType()) {
            // REFERENCE类型：从原始数据集获取最新文件
            return getRealTimeFiles(userRag.getOriginalRagId(), userId);
        } else {
            // SNAPSHOT类型：从版本快照获取固定文件
            return getSnapshotFiles(userRag.getRagVersionId());
        }
    }

    /** 获取用户可用的RAG文档单元列表
     * 
     * @param userId 用户ID
     * @param userRagId 用户RAG安装记录ID
     * @return 文档单元列表 */
    public List<DocumentUnitEntity> getRagDocuments(String userId, String userRagId) {
        UserRagEntity userRag = getUserRag(userId, userRagId);
        
        if (userRag.isReferenceType()) {
            // REFERENCE类型：从原始数据集获取最新文档
            return getRealTimeDocuments(userRag.getOriginalRagId(), userId);
        } else {
            // SNAPSHOT类型：从版本快照获取固定文档
            return getSnapshotDocuments(userRag.getRagVersionId());
        }
    }

    /** 获取用户可用的RAG文档单元列表（按文件ID过滤）
     * 
     * @param userId 用户ID
     * @param userRagId 用户RAG安装记录ID
     * @param fileId 文件ID
     * @return 文档单元列表 */
    public List<DocumentUnitEntity> getRagDocumentsByFile(String userId, String userRagId, String fileId) {
        UserRagEntity userRag = getUserRag(userId, userRagId);
        
        if (userRag.isReferenceType()) {
            // REFERENCE类型：从原始数据集获取最新文档
            return getRealTimeDocumentsByFile(fileId, userId);
        } else {
            // SNAPSHOT类型：从版本快照获取固定文档（需要找到对应的版本文件ID）
            return getSnapshotDocumentsByOriginalFile(userRag.getRagVersionId(), fileId);
        }
    }

    /** 检查用户是否可以访问指定RAG
     * 
     * @param userId 用户ID
     * @param userRagId 用户RAG安装记录ID
     * @return 是否可访问 */
    public boolean canAccessRag(String userId, String userRagId) {
        try {
            getUserRag(userId, userRagId);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    /** 获取RAG的实际数据来源信息
     * 
     * @param userId 用户ID
     * @param userRagId 用户RAG安装记录ID
     * @return 数据来源信息 */
    public RagDataSourceInfo getRagDataSourceInfo(String userId, String userRagId) {
        UserRagEntity userRag = getUserRag(userId, userRagId);
        
        RagDataSourceInfo sourceInfo = new RagDataSourceInfo();
        sourceInfo.setUserRagId(userRagId);
        sourceInfo.setOriginalRagId(userRag.getOriginalRagId());
        sourceInfo.setVersionId(userRag.getRagVersionId());
        sourceInfo.setInstallType(userRag.getInstallType());
        sourceInfo.setIsRealTime(userRag.isReferenceType());
        
        return sourceInfo;
    }

    // ========== 私有辅助方法 ==========

    /** 获取用户RAG安装记录 */
    private UserRagEntity getUserRag(String userId, String userRagId) {
        LambdaQueryWrapper<UserRagEntity> wrapper = Wrappers.<UserRagEntity>lambdaQuery()
                .eq(UserRagEntity::getUserId, userId)
                .eq(UserRagEntity::getId, userRagId);

        UserRagEntity userRag = userRagRepository.selectOne(wrapper);
        if (userRag == null) {
            throw new BusinessException("RAG不存在");
        }

        return userRag;
    }

    /** 获取实时文件（从原始数据集） */
    private List<FileDetailEntity> getRealTimeFiles(String originalRagId, String userId) {
        LambdaQueryWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getDataSetId, originalRagId)
                .eq(FileDetailEntity::getUserId, userId)
                .orderByDesc(FileDetailEntity::getCreatedAt);

        return fileDetailRepository.selectList(wrapper);
    }

    /** 获取快照文件（从版本快照） */
    private List<FileDetailEntity> getSnapshotFiles(String versionId) {
        // 这里需要根据实际的版本文件表结构来实现
        // 暂时返回空列表，具体实现需要根据RagVersionFileEntity的结构
        return List.of();
    }

    /** 获取实时文档（从原始数据集） */
    private List<DocumentUnitEntity> getRealTimeDocuments(String originalRagId, String userId) {
        // DocumentUnitEntity 可能没有直接的ragId和userId字段
        // 需要通过fileId关联查询，这里先返回空列表
        // TODO: 实现正确的文档查询逻辑
        return List.of();
    }

    /** 获取快照文档（从版本快照） */
    private List<DocumentUnitEntity> getSnapshotDocuments(String versionId) {
        // 这里需要根据实际的版本文档表结构来实现
        // 暂时返回空列表，具体实现需要根据RagVersionDocumentEntity的结构
        return List.of();
    }

    /** 获取实时文档（按文件ID过滤） */
    private List<DocumentUnitEntity> getRealTimeDocumentsByFile(String fileId, String userId) {
        LambdaQueryWrapper<DocumentUnitEntity> wrapper = Wrappers.<DocumentUnitEntity>lambdaQuery()
                .eq(DocumentUnitEntity::getFileId, fileId)
                .orderByDesc(DocumentUnitEntity::getCreatedAt);

        return documentUnitRepository.selectList(wrapper);
    }

    /** 获取快照文档（按原始文件ID过滤） */
    private List<DocumentUnitEntity> getSnapshotDocumentsByOriginalFile(String versionId, String originalFileId) {
        // 这里需要根据实际的版本文档表结构来实现
        // 需要先找到版本中对应的文件快照，再获取文档
        return List.of();
    }

    /** RAG数据来源信息 */
    public static class RagDataSourceInfo {
        private String userRagId;
        private String originalRagId;
        private String versionId;
        private InstallType installType;
        private Boolean isRealTime;

        public String getUserRagId() {
            return userRagId;
        }

        public void setUserRagId(String userRagId) {
            this.userRagId = userRagId;
        }

        public String getOriginalRagId() {
            return originalRagId;
        }

        public void setOriginalRagId(String originalRagId) {
            this.originalRagId = originalRagId;
        }

        public String getVersionId() {
            return versionId;
        }

        public void setVersionId(String versionId) {
            this.versionId = versionId;
        }

        public InstallType getInstallType() {
            return installType;
        }

        public void setInstallType(InstallType installType) {
            this.installType = installType;
        }

        public Boolean getIsRealTime() {
            return isRealTime;
        }

        public void setIsRealTime(Boolean isRealTime) {
            this.isRealTime = isRealTime;
        }
    }
}