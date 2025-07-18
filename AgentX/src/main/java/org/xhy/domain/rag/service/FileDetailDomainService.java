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
import org.xhy.domain.rag.constant.FileProcessingEventEnum;
import org.xhy.domain.rag.constant.FileProcessingStatusEnum;
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
    private final FileProcessingStateMachineService stateMachineService;

    public FileDetailDomainService(FileStorageService fileStorageService, 
                                   FileDetailRepository fileDetailRepository,
                                   FileProcessingStateMachineService stateMachineService) {
        this.fileStorageService = fileStorageService;
        this.fileDetailRepository = fileDetailRepository;
        this.stateMachineService = stateMachineService;
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
        
        // 设置初始状态为已上传
        fileDetailEntity.setProcessingStatus(FileProcessingStatusEnum.UPLOADED.getCode());
        
        // 初始化状态机
        stateMachineService.processFileState(fileDetailEntity);

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

    /** 开始文件OCR处理
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否成功开始处理 */
    public boolean startFileOcrProcessing(String fileId, String userId) {
        FileDetailEntity fileEntity = getFile(fileId, userId);
        boolean success = stateMachineService.handleEvent(fileEntity, FileProcessingEventEnum.START_OCR_PROCESSING);
        if (success) {
            updateFile(fileEntity);
        }
        return success;
    }

    /** 完成文件OCR处理
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否成功完成处理 */
    public boolean completeFileOcrProcessing(String fileId, String userId) {
        FileDetailEntity fileEntity = getFile(fileId, userId);
        boolean success = stateMachineService.handleEvent(fileEntity, FileProcessingEventEnum.COMPLETE_OCR_PROCESSING);
        if (success) {
            updateFile(fileEntity);
        }
        return success;
    }

    /** OCR处理失败
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否成功设置失败状态 */
    public boolean failFileOcrProcessing(String fileId, String userId) {
        FileDetailEntity fileEntity = getFile(fileId, userId);
        boolean success = stateMachineService.handleEvent(fileEntity, FileProcessingEventEnum.FAIL_OCR_PROCESSING);
        if (success) {
            updateFile(fileEntity);
        }
        return success;
    }

    /** 开始文件向量化处理
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否成功开始处理 */
    public boolean startFileEmbeddingProcessing(String fileId, String userId) {
        FileDetailEntity fileEntity = getFile(fileId, userId);
        boolean success = stateMachineService.handleEvent(fileEntity, FileProcessingEventEnum.START_EMBEDDING_PROCESSING);
        if (success) {
            updateFile(fileEntity);
        }
        return success;
    }

    /** 完成文件向量化处理
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否成功完成处理 */
    public boolean completeFileEmbeddingProcessing(String fileId, String userId) {
        FileDetailEntity fileEntity = getFile(fileId, userId);
        boolean success = stateMachineService.handleEvent(fileEntity, FileProcessingEventEnum.COMPLETE_EMBEDDING_PROCESSING);
        if (success) {
            updateFile(fileEntity);
        }
        return success;
    }

    /** 向量化处理失败
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否成功设置失败状态 */
    public boolean failFileEmbeddingProcessing(String fileId, String userId) {
        FileDetailEntity fileEntity = getFile(fileId, userId);
        boolean success = stateMachineService.handleEvent(fileEntity, FileProcessingEventEnum.FAIL_EMBEDDING_PROCESSING);
        if (success) {
            updateFile(fileEntity);
        }
        return success;
    }

    /** 重置文件处理状态
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否成功重置 */
    public boolean resetFileProcessing(String fileId, String userId) {
        FileDetailEntity fileEntity = getFile(fileId, userId);
        boolean success = stateMachineService.handleEvent(fileEntity, FileProcessingEventEnum.RESET_PROCESSING);
        if (success) {
            updateFile(fileEntity);
        }
        return success;
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
     * @param totalPages 总页数
     * @param userId 用户ID
     * @return 是否更新成功 */
    public boolean updateFileOcrProgress(String fileId, Integer currentOcrPage, Integer totalPages, String userId) {
        FileDetailEntity fileEntity = getFile(fileId, userId);
        boolean success = stateMachineService.updateOcrProgress(fileEntity, currentOcrPage, totalPages);
        if (success) {
            updateFile(fileEntity);
        }
        return success;
    }

    /** 更新文件向量化处理进度
     * @param fileId 文件ID
     * @param currentEmbeddingPage 当前向量化处理页数
     * @param totalPages 总页数
     * @param userId 用户ID
     * @return 是否更新成功 */
    public boolean updateFileEmbeddingProgress(String fileId, Integer currentEmbeddingPage, Integer totalPages, String userId) {
        FileDetailEntity fileEntity = getFile(fileId, userId);
        boolean success = stateMachineService.updateEmbeddingProgress(fileEntity, currentEmbeddingPage, totalPages);
        if (success) {
            updateFile(fileEntity);
        }
        return success;
    }

    /** 更新文件OCR处理进度（简化版本，兼容旧接口）
     * @param fileId 文件ID
     * @param currentOcrPage 当前OCR处理页数
     * @param ocrProgress OCR进度百分比
     * @deprecated 建议使用 updateFileOcrProgress(fileId, currentOcrPage, totalPages, userId) */
    @Deprecated
    public void updateFileOcrProgress(String fileId, Integer currentOcrPage, Double ocrProgress) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId).set(FileDetailEntity::getCurrentOcrPageNumber, currentOcrPage)
                .set(FileDetailEntity::getOcrProcessProgress, ocrProgress);
        fileDetailRepository.update(wrapper);
    }

    /** 更新文件向量化处理进度（简化版本，兼容旧接口）
     * @param fileId 文件ID
     * @param currentEmbeddingPage 当前向量化处理页数
     * @param embeddingProgress 向量化进度百分比
     * @deprecated 建议使用 updateFileEmbeddingProgress(fileId, currentEmbeddingPage, totalPages, userId) */
    @Deprecated
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

    /** 根据文件ID获取文件详情（无用户权限检查，用于MQ消费和状态机处理）
     * @param fileId 文件ID
     * @return 文件实体 */
    public FileDetailEntity getFileByIdWithoutUserCheck(String fileId) {
        FileDetailEntity fileEntity = fileDetailRepository.selectById(fileId);
        if (fileEntity == null) {
            throw new BusinessException("文件不存在");
        }
        
        // 确保文件有正确的状态
        if (fileEntity.getProcessingStatus() == null) {
            fileEntity.setProcessingStatus(FileProcessingStatusEnum.UPLOADED.getCode());
            stateMachineService.processFileState(fileEntity);
            // 更新到数据库
            LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                    .eq(FileDetailEntity::getId, fileId)
                    .set(FileDetailEntity::getProcessingStatus, fileEntity.getProcessingStatus());
            fileDetailRepository.update(wrapper);
        }
        
        return fileEntity;
    }
}
