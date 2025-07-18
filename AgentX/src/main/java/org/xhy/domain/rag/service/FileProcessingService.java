package org.xhy.domain.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.constant.FileProcessingEventEnum;
import org.xhy.domain.rag.constant.FileProcessingStatusEnum;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.domain.rag.straegy.RagDocSyncOcrStrategy;
import org.xhy.domain.rag.straegy.context.RagDocSyncOcrContext;
import org.xhy.infrastructure.exception.BusinessException;

/** 文件异步处理服务
 * @author zang
 * @date 2025-01-10 */
@Service
public class FileProcessingService {

    private final FileDetailRepository fileDetailRepository;
    private final RagDocSyncOcrContext ragDocSyncOcrContext;
    private final FileProcessingStateMachineService stateMachineService;

    public FileProcessingService(FileDetailRepository fileDetailRepository, 
                                 RagDocSyncOcrContext ragDocSyncOcrContext,
                                 FileProcessingStateMachineService stateMachineService) {
        this.fileDetailRepository = fileDetailRepository;
        this.ragDocSyncOcrContext = ragDocSyncOcrContext;
        this.stateMachineService = stateMachineService;
    }

    /** 异步处理文件初始化
     * @param fileId 文件ID
     * @param userId 用户ID */
    @Async
    public void processFileInitialization(String fileId, String userId) {
        try {
            FileDetailEntity fileEntity = getFileById(fileId, userId);
            
            // 使用状态机开始OCR处理
            boolean startSuccess = stateMachineService.handleEvent(fileEntity, FileProcessingEventEnum.START_OCR_PROCESSING);
            if (!startSuccess) {
                throw new BusinessException("无法开始OCR处理，当前状态不允许");
            }
            
            // 更新文件状态到数据库
            updateFileInDatabase(fileEntity);

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

            // 重新获取文件状态（可能在策略处理中被更新）
            fileEntity = getFileById(fileId, userId);
            
            // 完成OCR处理
            boolean completeSuccess = stateMachineService.handleEvent(fileEntity, FileProcessingEventEnum.COMPLETE_OCR_PROCESSING);
            if (completeSuccess) {
                updateFileInDatabase(fileEntity);
            }

        } catch (Exception e) {
            // 处理失败
            try {
                FileDetailEntity fileEntity = getFileById(fileId, userId);
                boolean failSuccess = stateMachineService.handleEvent(fileEntity, FileProcessingEventEnum.FAIL_OCR_PROCESSING);
                if (failSuccess) {
                    updateFileInDatabase(fileEntity);
                }
            } catch (Exception ex) {
                // 记录错误但不抛出异常
                // logger.error("更新文件失败状态时发生错误", ex);
            }
        }
    }

    /** 根据文件ID获取文件详情
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件实体 */
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

    /** 更新文件状态到数据库
     * @param fileEntity 文件实体 */
    private void updateFileInDatabase(FileDetailEntity fileEntity) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileEntity.getId())
                .set(FileDetailEntity::getProcessingStatus, fileEntity.getProcessingStatus())
                .set(FileDetailEntity::getCurrentOcrPageNumber, fileEntity.getCurrentOcrPageNumber())
                .set(FileDetailEntity::getCurrentEmbeddingPageNumber, fileEntity.getCurrentEmbeddingPageNumber())
                .set(FileDetailEntity::getOcrProcessProgress, fileEntity.getOcrProcessProgress())
                .set(FileDetailEntity::getEmbeddingProcessProgress, fileEntity.getEmbeddingProcessProgress());
        fileDetailRepository.update(wrapper);
    }

    /** 更新文件处理进度（通过状态机）
     * @param fileId 文件ID
     * @param currentOcrPage 当前OCR页数
     * @param totalPages 总页数
     * @param userId 用户ID */
    public void updateFileOcrProgressViaStateMachine(String fileId, Integer currentOcrPage, Integer totalPages, String userId) {
        FileDetailEntity fileEntity = getFileById(fileId, userId);
        boolean success = stateMachineService.updateOcrProgress(fileEntity, currentOcrPage, totalPages);
        if (success) {
            updateFileInDatabase(fileEntity);
        }
    }

    /** 更新文件向量化进度（通过状态机）
     * @param fileId 文件ID
     * @param currentEmbeddingPage 当前向量化页数
     * @param totalPages 总页数
     * @param userId 用户ID */
    public void updateFileEmbeddingProgressViaStateMachine(String fileId, Integer currentEmbeddingPage, Integer totalPages, String userId) {
        FileDetailEntity fileEntity = getFileById(fileId, userId);
        boolean success = stateMachineService.updateEmbeddingProgress(fileEntity, currentEmbeddingPage, totalPages);
        if (success) {
            updateFileInDatabase(fileEntity);
        }
    }

    /** 更新文件处理进度（已弃用，使用分离的OCR/向量化进度方法）
     * @param fileId 文件ID
     * @param currentPage 当前处理页数
     * @param progress 进度百分比
     * @deprecated 请使用 updateFileOcrProgress 或 updateFileEmbeddingProgress */
    @Deprecated
    private void updateFileProgress(String fileId, Integer currentPage, Double progress) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId).set(FileDetailEntity::getCurrentOcrPageNumber, currentPage)
                .set(FileDetailEntity::getOcrProcessProgress, progress);
        fileDetailRepository.update(wrapper);
    }

    /** 更新文件OCR处理进度
     * @param fileId 文件ID
     * @param currentOcrPage 当前OCR处理页数
     * @param ocrProgress OCR进度百分比 */
    private void updateFileOcrProgress(String fileId, Integer currentOcrPage, Double ocrProgress) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId).set(FileDetailEntity::getCurrentOcrPageNumber, currentOcrPage)
                .set(FileDetailEntity::getOcrProcessProgress, ocrProgress);
        fileDetailRepository.update(wrapper);
    }

    /** 更新文件向量化处理进度
     * @param fileId 文件ID
     * @param currentEmbeddingPage 当前向量化处理页数
     * @param embeddingProgress 向量化进度百分比 */
    private void updateFileEmbeddingProgress(String fileId, Integer currentEmbeddingPage, Double embeddingProgress) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId)
                .set(FileDetailEntity::getCurrentEmbeddingPageNumber, currentEmbeddingPage)
                .set(FileDetailEntity::getEmbeddingProcessProgress, embeddingProgress);
        fileDetailRepository.update(wrapper);
    }

    /** 更新文件总页数
     * @param fileId 文件ID
     * @param totalPages 总页数 */
    private void updateFilePageSize(String fileId, Integer totalPages) {
        LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                .eq(FileDetailEntity::getId, fileId).set(FileDetailEntity::getFilePageSize, totalPages);
        fileDetailRepository.update(wrapper);
    }

    /** 获取文件处理进度详情
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 处理进度信息 */
    public String getProcessingStatus(String fileId, String userId) {
        FileDetailEntity fileEntity = getFileById(fileId, userId);

        Integer processingStatus = fileEntity.getProcessingStatus();
        Integer currentOcrPage = fileEntity.getCurrentOcrPageNumber();
        Integer currentEmbeddingPage = fileEntity.getCurrentEmbeddingPageNumber();
        Integer totalPages = fileEntity.getFilePageSize();
        Double ocrProgress = fileEntity.getOcrProcessProgress();
        Double embeddingProgress = fileEntity.getEmbeddingProcessProgress();

        StringBuilder status = new StringBuilder();
        status.append("文件: ").append(fileEntity.getOriginalFilename()).append("\n");

        if (processingStatus != null) {
            FileProcessingStatusEnum statusEnum = FileProcessingStatusEnum.fromCode(processingStatus);
            status.append("状态: ").append(statusEnum.getDescription()).append("\n");

            switch (statusEnum) {
                case OCR_PROCESSING:
                    if (currentOcrPage != null && totalPages != null) {
                        status.append("OCR进度: ").append(currentOcrPage).append("/").append(totalPages).append(" 页");
                        if (ocrProgress != null) {
                            status.append(" (").append(String.format("%.1f", ocrProgress)).append("%)");
                        }
                    }
                    break;
                case EMBEDDING_PROCESSING:
                    if (currentEmbeddingPage != null && totalPages != null) {
                        status.append("向量化进度: ").append(currentEmbeddingPage).append("/").append(totalPages).append(" 页");
                        if (embeddingProgress != null) {
                            status.append(" (").append(String.format("%.1f", embeddingProgress)).append("%)");
                        }
                    }
                    break;
                case COMPLETED:
                    status.append("处理完成");
                    break;
                case OCR_FAILED:
                case EMBEDDING_FAILED:
                    status.append("处理失败，请重试");
                    break;
                default:
                    // 显示详细进度信息
                    if (currentOcrPage != null && totalPages != null) {
                        status.append("OCR进度: ").append(currentOcrPage).append("/").append(totalPages).append(" 页");
                        if (ocrProgress != null) {
                            status.append(" (").append(String.format("%.1f", ocrProgress)).append("%)");
                        }
                        status.append("\n");
                    }
                    if (currentEmbeddingPage != null && totalPages != null) {
                        status.append("向量化进度: ").append(currentEmbeddingPage).append("/").append(totalPages).append(" 页");
                        if (embeddingProgress != null) {
                            status.append(" (").append(String.format("%.1f", embeddingProgress)).append("%)");
                        }
                    }
                    break;
            }
        } else {
            status.append("状态: 未知");
        }

        return status.toString();
    }
}