package org.xhy.domain.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.constant.FileInitializeStatus;
import org.xhy.domain.rag.constant.EmbeddingStatus;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.domain.rag.straegy.RagDocSyncOcrStrategy;
import org.xhy.domain.rag.straegy.context.RagDocSyncOcrContext;
import org.xhy.infrastructure.exception.BusinessException;

/**
 * 文件异步处理服务
 * @author zang
 * @date 2025-01-10
 */
@Service
public class FileProcessingService {

    private final FileDetailRepository fileDetailRepository;
    private final RagDocSyncOcrContext ragDocSyncOcrContext;

    public FileProcessingService(FileDetailRepository fileDetailRepository,
                                RagDocSyncOcrContext ragDocSyncOcrContext) {
        this.fileDetailRepository = fileDetailRepository;
        this.ragDocSyncOcrContext = ragDocSyncOcrContext;
    }

    /**
     * 异步处理文件初始化
     * @param fileId 文件ID
     * @param userId 用户ID
     */
    @Async
    public void processFileInitialization(String fileId, String userId) {
        try {
            // 更新状态为初始化中
            updateFileInitializeStatus(fileId, FileInitializeStatus.INITIALIZING);
            updateFileProgress(fileId, 0, 0.0);
            
            FileDetailEntity fileEntity = getFileById(fileId, userId);
            
            // 根据文件扩展名选择处理策略
            String fileExt = fileEntity.getExt();
            if (fileExt == null) {
                throw new BusinessException("文件扩展名不能为空");
            }
            
            RagDocSyncOcrStrategy strategy = ragDocSyncOcrContext.getTaskExportStrategy(fileExt.toUpperCase());
            if (strategy == null) {
                throw new BusinessException("不支持的文件类型: " + fileExt);
            }
            
            // 创建处理消息
            RagDocSyncOcrMessage message = new RagDocSyncOcrMessage();
            message.setFileId(fileId);
            message.setPageSize(fileEntity.getFilePageSize());
            
            // 执行初始化处理 - 策略会自动调用pushPageSize获取总页数并更新进度
            strategy.handle(message, fileExt.toUpperCase());
            
            // 完成初始化
            updateFileInitializeStatus(fileId, FileInitializeStatus.INITIALIZED);
            
        } catch (Exception e) {
            // 处理失败
            updateFileInitializeStatus(fileId, FileInitializeStatus.INITIALIZATION_FAILED);
            updateFileProgress(fileId, 0, 0.0);
        }
    }

    /**
     * 异步处理文件向量化
     * @param fileId 文件ID
     * @param userId 用户ID
     */
    @Async
    public void processFileEmbedding(String fileId, String userId) {
        try {
            FileDetailEntity fileEntity = getFileById(fileId, userId);
            
            // 模拟向量化处理过程
            simulateEmbeddingProcessing(fileId);
            
            // 完成向量化
            updateFileEmbeddingStatus(fileId, EmbeddingStatus.INITIALIZED);
            updateFileProgress(fileId, fileEntity.getFilePageSize(), 100.0);
            
        } catch (Exception e) {
            // 处理失败
            updateFileEmbeddingStatus(fileId, EmbeddingStatus.INITIALIZATION_FAILED);
            updateFileProgress(fileId, 0, 0.0);
        }
    }

    /**
     * 模拟向量化处理过程，实时更新进度
     * @param fileId 文件ID
     */
    private void simulateEmbeddingProcessing(String fileId) throws InterruptedException {
        FileDetailEntity fileEntity = getFileById(fileId, null);
        Integer totalPages = fileEntity.getFilePageSize();
        
        if (totalPages == null || totalPages <= 0) {
            totalPages = 1;
            updateFilePageSize(fileId, totalPages);
        }

        // 模拟向量化处理
        for (int currentPage = 1; currentPage <= totalPages; currentPage++) {
            Thread.sleep(1000); // 每页处理1秒
            
            double progress = (double) currentPage / totalPages * 100.0;
            updateFileProgress(fileId, currentPage, progress);
        }
    }

    /**
     * 根据文件ID获取文件详情
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件实体
     */
    private FileDetailEntity getFileById(String fileId, String userId) {
        LambdaQueryWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaQuery()
                .eq(FileDetailEntity::getId, fileId);
        if (userId != null) {
            wrapper.eq(FileDetailEntity::getUserId, userId);
        }
        FileDetailEntity fileEntity = fileDetailRepository.selectOne(wrapper);
        if (fileEntity == null) {
            throw new BusinessException("文件不存在或无权限访问");
        }
        return fileEntity;
    }

    /**
     * 更新文件的初始化状态
     * @param fileId 文件ID
     * @param status 初始化状态
     */
    private void updateFileInitializeStatus(String fileId, Integer status) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId)
                .set(FileDetailEntity::getIsInitialize, status);
        fileDetailRepository.update(wrapper);
    }

    /**
     * 更新文件的向量化状态
     * @param fileId 文件ID
     * @param status 向量化状态
     */
    private void updateFileEmbeddingStatus(String fileId, Integer status) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId)
                .set(FileDetailEntity::getIsEmbedding, status);
        fileDetailRepository.update(wrapper);
    }

    /**
     * 更新文件处理进度
     * @param fileId 文件ID
     * @param currentPage 当前处理页数
     * @param progress 进度百分比
     */
    private void updateFileProgress(String fileId, Integer currentPage, Double progress) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId)
                .set(FileDetailEntity::getCurrentPageNumber, currentPage)
                .set(FileDetailEntity::getProcessProgress, progress);
        fileDetailRepository.update(wrapper);
    }

    /**
     * 更新文件总页数
     * @param fileId 文件ID
     * @param totalPages 总页数
     */
    private void updateFilePageSize(String fileId, Integer totalPages) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId)
                .set(FileDetailEntity::getFilePageSize, totalPages);
        fileDetailRepository.update(wrapper);
    }

    /**
     * 获取文件处理进度详情
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 处理进度信息
     */
    public String getProcessingStatus(String fileId, String userId) {
        FileDetailEntity fileEntity = getFileById(fileId, userId);
        
        Integer initStatus = fileEntity.getIsInitialize();
        Integer embeddingStatus = fileEntity.getIsEmbedding();
        Integer currentPage = fileEntity.getCurrentPageNumber();
        Integer totalPages = fileEntity.getFilePageSize();
        Double progress = fileEntity.getProcessProgress();
        
        StringBuilder status = new StringBuilder();
        status.append("文件: ").append(fileEntity.getOriginalFilename()).append("\n");
        
        if (initStatus != null && initStatus == FileInitializeStatus.INITIALIZING) {
            status.append("状态: 初始化中\n");
            if (currentPage != null && totalPages != null) {
                status.append("进度: ").append(currentPage).append("/").append(totalPages).append(" 页");
                if (progress != null) {
                    status.append(" (").append(String.format("%.1f", progress)).append("%)");
                }
            }
        } else if (embeddingStatus != null && embeddingStatus == EmbeddingStatus.INITIALIZING) {
            status.append("状态: 向量化中\n");
            if (currentPage != null && totalPages != null) {
                status.append("进度: ").append(currentPage).append("/").append(totalPages).append(" 页");
                if (progress != null) {
                    status.append(" (").append(String.format("%.1f", progress)).append("%)");
                }
            }
        } else {
            status.append("状态: 处理完成或待处理");
        }
        
        return status.toString();
    }
}