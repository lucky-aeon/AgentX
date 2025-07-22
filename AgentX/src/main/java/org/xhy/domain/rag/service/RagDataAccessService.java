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
    private final UserRagFileRepository userRagFileRepository;
    private final UserRagDocumentRepository userRagDocumentRepository;

    public RagDataAccessService(UserRagRepository userRagRepository, FileDetailRepository fileDetailRepository,
            DocumentUnitRepository documentUnitRepository, RagVersionFileRepository ragVersionFileRepository,
            RagVersionDocumentRepository ragVersionDocumentRepository, UserRagFileRepository userRagFileRepository,
            UserRagDocumentRepository userRagDocumentRepository) {
        this.userRagRepository = userRagRepository;
        this.fileDetailRepository = fileDetailRepository;
        this.documentUnitRepository = documentUnitRepository;
        this.ragVersionFileRepository = ragVersionFileRepository;
        this.ragVersionDocumentRepository = ragVersionDocumentRepository;
        this.userRagFileRepository = userRagFileRepository;
        this.userRagDocumentRepository = userRagDocumentRepository;
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
            // SNAPSHOT类型：从用户快照获取固定文件
            return getUserSnapshotFiles(userRagId);
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
            // SNAPSHOT类型：从用户快照获取固定文档
            return getUserSnapshotDocuments(userRagId);
        }
    }

    /** 获取用户可用的RAG文件信息
     * 
     * @param userId 用户ID
     * @param userRagId 用户RAG安装记录ID
     * @param fileId 文件ID
     * @return 文件详细信息 */
    public FileDetailEntity getRagFileInfo(String userId, String userRagId, String fileId) {
        UserRagEntity userRag = getUserRag(userId, userRagId);

        if (userRag.isReferenceType()) {
            // REFERENCE类型：从原始数据集获取最新文件信息
            return getRealTimeFileInfo(fileId, userId);
        } else {
            // SNAPSHOT类型：从用户快照获取文件信息
            return getUserSnapshotFileInfo(userRagId, fileId);
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
            // SNAPSHOT类型：从用户快照获取固定文档（fileId就是用户快照文件ID）
            return getUserSnapshotDocumentsByUserFileId(userRagId, fileId);
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
                .eq(UserRagEntity::getUserId, userId).eq(UserRagEntity::getId, userRagId);

        UserRagEntity userRag = userRagRepository.selectOne(wrapper);
        if (userRag == null) {
            throw new BusinessException("RAG不存在");
        }

        return userRag;
    }

    /** 获取实时文件（从原始数据集） */
    private List<FileDetailEntity> getRealTimeFiles(String originalRagId, String userId) {
        LambdaQueryWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getDataSetId, originalRagId).eq(FileDetailEntity::getUserId, userId)
                .orderByDesc(FileDetailEntity::getCreatedAt);

        return fileDetailRepository.selectList(wrapper);
    }

    /** 获取用户快照文件（从用户快照表） */
    private List<FileDetailEntity> getUserSnapshotFiles(String userRagId) {
        LambdaQueryWrapper<UserRagFileEntity> wrapper = Wrappers.<UserRagFileEntity>lambdaQuery()
                .eq(UserRagFileEntity::getUserRagId, userRagId).orderByDesc(UserRagFileEntity::getCreatedAt);

        List<UserRagFileEntity> userFiles = userRagFileRepository.selectList(wrapper);

        // 转换为FileDetailEntity格式（用于兼容现有接口）
        return userFiles.stream().map(this::convertToFileDetailEntity).collect(java.util.stream.Collectors.toList());
    }

    /** 获取实时文档（从原始数据集） */
    private List<DocumentUnitEntity> getRealTimeDocuments(String originalRagId, String userId) {
        // DocumentUnitEntity 可能没有直接的ragId和userId字段
        // 需要通过fileId关联查询，这里先返回空列表
        // TODO: 实现正确的文档查询逻辑
        return List.of();
    }

    /** 获取用户快照文档（从用户快照表） */
    private List<DocumentUnitEntity> getUserSnapshotDocuments(String userRagId) {
        LambdaQueryWrapper<UserRagDocumentEntity> wrapper = Wrappers.<UserRagDocumentEntity>lambdaQuery()
                .eq(UserRagDocumentEntity::getUserRagId, userRagId).orderByDesc(UserRagDocumentEntity::getCreatedAt);

        List<UserRagDocumentEntity> userDocs = userRagDocumentRepository.selectList(wrapper);

        // 转换为DocumentUnitEntity格式（用于兼容现有接口）
        return userDocs.stream().map(this::convertToDocumentUnitEntity).collect(java.util.stream.Collectors.toList());
    }

    /** 获取实时文件信息 */
    private FileDetailEntity getRealTimeFileInfo(String fileId, String userId) {
        LambdaQueryWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getId, fileId)
                .eq(FileDetailEntity::getUserId, userId);
        
        FileDetailEntity file = fileDetailRepository.selectOne(wrapper);
        if (file == null) {
            throw new BusinessException("文件不存在或无权限访问");
        }
        return file;
    }

    /** 获取用户快照文件信息 */
    private FileDetailEntity getUserSnapshotFileInfo(String userRagId, String userFileId) {
        // 验证文件属于指定的用户RAG
        LambdaQueryWrapper<UserRagFileEntity> wrapper = Wrappers.<UserRagFileEntity>lambdaQuery()
                .eq(UserRagFileEntity::getUserRagId, userRagId)
                .eq(UserRagFileEntity::getId, userFileId);
        
        UserRagFileEntity userFile = userRagFileRepository.selectOne(wrapper);
        if (userFile == null) {
            throw new BusinessException("文件不存在或无权限访问");
        }
        
        // 动态计算实际页数 - 查询最大页码
        LambdaQueryWrapper<UserRagDocumentEntity> docWrapper = Wrappers.<UserRagDocumentEntity>lambdaQuery()
                .eq(UserRagDocumentEntity::getUserRagFileId, userFileId)
                .select(UserRagDocumentEntity::getPage)
                .orderByDesc(UserRagDocumentEntity::getPage)
                .last("LIMIT 1");
        
        List<UserRagDocumentEntity> docs = userRagDocumentRepository.selectList(docWrapper);
        int actualPageSize = docs.isEmpty() ? 0 : docs.get(0).getPage() + 1;
        
        // 转换为FileDetailEntity并修正页数
        FileDetailEntity file = convertToFileDetailEntity(userFile);
        file.setFilePageSize(actualPageSize); // 使用动态计算的页数
        return file;
    }

    /** 获取实时文档（按文件ID过滤） */
    private List<DocumentUnitEntity> getRealTimeDocumentsByFile(String fileId, String userId) {
        LambdaQueryWrapper<DocumentUnitEntity> wrapper = Wrappers.<DocumentUnitEntity>lambdaQuery()
                .eq(DocumentUnitEntity::getFileId, fileId).orderByDesc(DocumentUnitEntity::getCreatedAt);

        return documentUnitRepository.selectList(wrapper);
    }

    /** 获取用户快照文档（按用户文件ID过滤） */
    private List<DocumentUnitEntity> getUserSnapshotDocumentsByUserFileId(String userRagId, String userFileId) {
        // 验证文件属于指定的用户RAG
        LambdaQueryWrapper<UserRagFileEntity> fileWrapper = Wrappers.<UserRagFileEntity>lambdaQuery()
                .eq(UserRagFileEntity::getUserRagId, userRagId)
                .eq(UserRagFileEntity::getId, userFileId);

        UserRagFileEntity userFile = userRagFileRepository.selectOne(fileWrapper);
        if (userFile == null) {
            return List.of();
        }

        // 查询对应的文档快照
        LambdaQueryWrapper<UserRagDocumentEntity> docWrapper = Wrappers.<UserRagDocumentEntity>lambdaQuery()
                .eq(UserRagDocumentEntity::getUserRagId, userRagId)
                .eq(UserRagDocumentEntity::getUserRagFileId, userFileId)
                .orderByDesc(UserRagDocumentEntity::getCreatedAt);

        List<UserRagDocumentEntity> userDocs = userRagDocumentRepository.selectList(docWrapper);

        // 转换为DocumentUnitEntity格式
        return userDocs.stream().map(this::convertToDocumentUnitEntity).collect(java.util.stream.Collectors.toList());
    }

    /** 获取用户快照文档（按原始文件ID过滤） */
    private List<DocumentUnitEntity> getUserSnapshotDocumentsByOriginalFile(String userRagId, String originalFileId) {
        // 先找到对应的用户文件快照
        LambdaQueryWrapper<UserRagFileEntity> fileWrapper = Wrappers.<UserRagFileEntity>lambdaQuery()
                .eq(UserRagFileEntity::getUserRagId, userRagId)
                .eq(UserRagFileEntity::getOriginalFileId, originalFileId);

        UserRagFileEntity userFile = userRagFileRepository.selectOne(fileWrapper);
        if (userFile == null) {
            return List.of();
        }

        // 再查询对应的文档快照
        LambdaQueryWrapper<UserRagDocumentEntity> docWrapper = Wrappers.<UserRagDocumentEntity>lambdaQuery()
                .eq(UserRagDocumentEntity::getUserRagId, userRagId)
                .eq(UserRagDocumentEntity::getUserRagFileId, userFile.getId())
                .orderByDesc(UserRagDocumentEntity::getCreatedAt);

        List<UserRagDocumentEntity> userDocs = userRagDocumentRepository.selectList(docWrapper);

        // 转换为DocumentUnitEntity格式
        return userDocs.stream().map(this::convertToDocumentUnitEntity).collect(java.util.stream.Collectors.toList());
    }

    // ========== 转换方法 ==========

    /** 转换用户文件快照为FileDetailEntity格式 */
    private FileDetailEntity convertToFileDetailEntity(UserRagFileEntity userFile) {
        FileDetailEntity file = new FileDetailEntity();
        file.setId(userFile.getId()); // 使用用户文件快照的ID
        file.setDataSetId(userFile.getUserRagId()); // 设置为userRagId，表示数据来源
        file.setOriginalFilename(userFile.getFileName());
        file.setSize(userFile.getFileSize());
        file.setFilePageSize(userFile.getFilePageSize()); // 设置文件页数
        file.setExt(userFile.getFileType());
        file.setPath(userFile.getFilePath());
        file.setProcessingStatus(userFile.getProcessStatus()); // 设置处理状态
        file.setCreatedAt(userFile.getCreatedAt());
        file.setUpdatedAt(userFile.getUpdatedAt());
        return file;
    }

    /** 转换用户文档快照为DocumentUnitEntity格式 */
    private DocumentUnitEntity convertToDocumentUnitEntity(UserRagDocumentEntity userDoc) {
        DocumentUnitEntity doc = new DocumentUnitEntity();
        doc.setId(userDoc.getId()); // 使用用户文档快照的ID
        doc.setFileId(userDoc.getUserRagFileId()); // 设置为用户文件快照ID
        doc.setContent(userDoc.getContent());
        doc.setPage(userDoc.getPage());
        doc.setCreatedAt(userDoc.getCreatedAt());
        doc.setUpdatedAt(userDoc.getUpdatedAt());
        return doc;
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