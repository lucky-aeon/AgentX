package org.xhy.domain.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;

/** 文件详情领域服务
 * 
 * @author shilong.zang
 * @date 23:38 <br/>
 */
@Service
public class FileDetailDomainService {

    private final FileStorageService fileStorageService;
    private final FileDetailRepository fileDetailRepository;

    public FileDetailDomainService(FileStorageService fileStorageService, FileDetailRepository fileDetailRepository) {
        this.fileStorageService = fileStorageService;
        this.fileDetailRepository = fileDetailRepository;
    }

    /** 上传文件到指定数据集
     * @param fileDetailEntity 文件详情实体
     * @return 上传后的文件信息 */
    public FileDetailEntity uploadFileToDataset(FileDetailEntity fileDetailEntity) {
        if (fileDetailEntity.getMultipartFile() == null) {
            throw new BusinessException("上传文件不能为空");
        }

        if (StringUtils.isBlank(fileDetailEntity.getDataSetId())) {
            throw new BusinessException("数据集ID不能为空");
        }

        final FileInfo upload = fileStorageService.of(fileDetailEntity.getMultipartFile())
                .setMetadata(Map.of("dataset", fileDetailEntity.getDataSetId(), "userid", fileDetailEntity.getUserId()))
                .upload();

        // 设置文件基本信息
        fileDetailEntity.setId(upload.getId());
        fileDetailEntity.setUrl(upload.getUrl());
        fileDetailEntity.setSize(upload.getSize());
        fileDetailEntity.setFilename(upload.getFilename());
        fileDetailEntity.setOriginalFilename(upload.getOriginalFilename());
        fileDetailEntity.setPath(upload.getPath());
        fileDetailEntity.setExt(upload.getExt());
        fileDetailEntity.setContentType(upload.getContentType());
        fileDetailEntity.setPlatform(upload.getPlatform());

        // 保存文件记录
        // fileDetailRepository.insert(fileDetailEntity);
        return fileDetailEntity;
    }

    /** 根据ID获取文件详情
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件详情实体 */
    public FileDetailEntity getFile(String fileId, String userId) {
        LambdaQueryWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getId, fileId).eq(FileDetailEntity::getUserId, userId);
        FileDetailEntity file = fileDetailRepository.selectOne(wrapper);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        return file;
    }

    /** 查找文件详情（可返回null）
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件详情实体或null */
    public FileDetailEntity findFile(String fileId, String userId) {
        LambdaQueryWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getId, fileId).eq(FileDetailEntity::getUserId, userId);
        return fileDetailRepository.selectOne(wrapper);
    }

    /** 检查文件是否存在
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否存在 */
    public boolean existsFile(String fileId, String userId) {
        LambdaQueryWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getId, fileId).eq(FileDetailEntity::getUserId, userId);
        return fileDetailRepository.exists(wrapper);
    }

    /** 检查文件存在性，不存在则抛出异常
     * @param fileId 文件ID
     * @param userId 用户ID */
    public void checkFileExists(String fileId, String userId) {
        if (!existsFile(fileId, userId)) {
            throw new BusinessException("文件不存在");
        }
    }

    /** 删除文件
     * @param fileId 文件ID
     * @param userId 用户ID */
    public void deleteFile(String fileId, String userId) {
        // 获取文件信息
        FileDetailEntity file = getFile(fileId, userId);

        // 从文件存储服务删除文件
        try {
            fileStorageService.delete(file.getUrl());
        } catch (Exception e) {
            // 记录日志但不影响数据库删除
            // log.warn("删除存储文件失败: {}", file.getUrl(), e);
        }

    }

    /** 更新文件信息
     * @param fileDetail 文件详情实体 */
    public void updateFile(FileDetailEntity fileDetail) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileDetail.getId())
                .eq(FileDetailEntity::getUserId, fileDetail.getUserId());
        fileDetailRepository.checkedUpdate(fileDetail, wrapper);
    }

    /** 分页查询数据集下的文件
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @return 分页结果 */
    public IPage<FileDetailEntity> listFilesByDataset(String datasetId, String userId, Integer page, Integer pageSize,
            String keyword) {
        LambdaQueryWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getDataSetId, datasetId).eq(FileDetailEntity::getUserId, userId);

        // 关键词搜索
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(FileDetailEntity::getOriginalFilename, keyword).or()
                    .like(FileDetailEntity::getFilename, keyword));
        }

        wrapper.orderByDesc(FileDetailEntity::getCreatedAt);

        Page<FileDetailEntity> pageObj = new Page<>(page, pageSize);
        return fileDetailRepository.selectPage(pageObj, wrapper);
    }

    /** 获取数据集下的所有文件
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 文件列表 */
    public List<FileDetailEntity> listAllFilesByDataset(String datasetId, String userId) {
        LambdaQueryWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getDataSetId, datasetId).eq(FileDetailEntity::getUserId, userId)
                .orderByDesc(FileDetailEntity::getCreatedAt);
        return fileDetailRepository.selectList(wrapper);
    }

    /** 统计数据集下的文件数量
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @return 文件数量 */
    public long countFilesByDataset(String datasetId, String userId) {
        LambdaQueryWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getDataSetId, datasetId).eq(FileDetailEntity::getUserId, userId);
        return fileDetailRepository.selectCount(wrapper);
    }

    /** 批量删除数据集下的所有文件
     * @param datasetId 数据集ID
     * @param userId 用户ID */
    public void deleteAllFilesByDataset(String datasetId, String userId) {
        // 获取所有文件
        List<FileDetailEntity> files = listAllFilesByDataset(datasetId, userId);

        // 删除存储文件
        for (FileDetailEntity file : files) {
            try {
                fileStorageService.delete(file.getUrl());
            } catch (Exception e) {
                // 记录日志但继续删除
                // log.warn("删除存储文件失败: {}", file.getUrl(), e);
            }
        }

    }

    /** 更新文件的初始化状态
     * @param fileId 文件ID
     * @param status 初始化状态 */
    public void updateFileInitializeStatus(String fileId, Integer status) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId).set(FileDetailEntity::getIsInitialize, status);
        fileDetailRepository.update(wrapper);
    }

    /** 更新文件的向量化状态
     * @param fileId 文件ID
     * @param status 向量化状态 */
    public void updateFileEmbeddingStatus(String fileId, Integer status) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId).set(FileDetailEntity::getIsEmbedding, status);
        fileDetailRepository.update(wrapper);
    }

    /** 根据文件ID获取文件详情
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件实体 */
    public FileDetailEntity getFileById(String fileId, String userId) {
        LambdaQueryWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getId, fileId).eq(FileDetailEntity::getUserId, userId);
        FileDetailEntity fileEntity = fileDetailRepository.selectOne(wrapper);
        if (fileEntity == null) {
            throw new BusinessException("文件不存在或无权限访问");
        }
        return fileEntity;
    }

    /** 更新文件处理进度（已弃用，使用分离的OCR/向量化进度方法）
     * @param fileId 文件ID
     * @param currentPage 当前处理页数
     * @param progress 进度百分比
     * @deprecated 请使用 updateFileOcrProgress 或 updateFileEmbeddingProgress */
    @Deprecated
    public void updateFileProgress(String fileId, Integer currentPage, Double progress) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId).set(FileDetailEntity::getCurrentOcrPageNumber, currentPage)
                .set(FileDetailEntity::getOcrProcessProgress, progress);
        fileDetailRepository.update(wrapper);
    }

    /** 更新文件OCR处理进度
     * @param fileId 文件ID
     * @param currentOcrPage 当前OCR处理页数
     * @param ocrProgress OCR进度百分比 */
    public void updateFileOcrProgress(String fileId, Integer currentOcrPage, Double ocrProgress) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId).set(FileDetailEntity::getCurrentOcrPageNumber, currentOcrPage)
                .set(FileDetailEntity::getOcrProcessProgress, ocrProgress);
        fileDetailRepository.update(wrapper);
    }

    /** 更新文件向量化处理进度
     * @param fileId 文件ID
     * @param currentEmbeddingPage 当前向量化处理页数
     * @param embeddingProgress 向量化进度百分比 */
    public void updateFileEmbeddingProgress(String fileId, Integer currentEmbeddingPage, Double embeddingProgress) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId)
                .set(FileDetailEntity::getCurrentEmbeddingPageNumber, currentEmbeddingPage)
                .set(FileDetailEntity::getEmbeddingProcessProgress, embeddingProgress);
        fileDetailRepository.update(wrapper);
    }

    /** 更新文件总页数
     * @param fileId 文件ID
     * @param totalPages 总页数 */
    public void updateFilePageSize(String fileId, Integer totalPages) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId).set(FileDetailEntity::getFilePageSize, totalPages);
        fileDetailRepository.update(wrapper);
    }

    /** 获取文件扩展名
     * @param fileId 文件ID
     * @return 文件扩展名 */
    public String getFileExtension(String fileId) {
        LambdaQueryWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getId, fileId).select(FileDetailEntity::getExt);
        FileDetailEntity fileEntity = fileDetailRepository.selectOne(wrapper);
        if (fileEntity == null) {
            throw new BusinessException("文件不存在");
        }
        return fileEntity.getExt();
    }

    /** 根据文件ID获取文件详情（无用户权限检查，用于MQ消费）
     * @param fileId 文件ID
     * @return 文件实体 */
    public FileDetailEntity getFileByIdWithoutUserCheck(String fileId) {
        FileDetailEntity fileEntity = fileDetailRepository.selectById(fileId);
        if (fileEntity == null) {
            throw new BusinessException("文件不存在");
        }
        return fileEntity;
    }
}
