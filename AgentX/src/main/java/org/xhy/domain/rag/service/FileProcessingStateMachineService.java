package org.xhy.domain.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.constant.FileProcessingEventEnum;
import org.xhy.domain.rag.constant.FileProcessingStatusEnum;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.service.state.FileProcessingStateProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 文件处理状态机服务
 * 
 * @author zang
 * @date 2025-07-17 */
@Service
public class FileProcessingStateMachineService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingStateMachineService.class);

    private final Map<Integer, FileProcessingStateProcessor> processorMap = new HashMap<>();

    public FileProcessingStateMachineService(List<FileProcessingStateProcessor> processors) {
        for (FileProcessingStateProcessor processor : processors) {
            processorMap.put(processor.getStatus(), processor);
        }
        logger.info("文件处理状态机初始化完成，注册了 {} 个状态处理器", processorMap.size());
    }

    /** 处理文件状态
     * 
     * @param fileEntity 文件实体
     * @return 是否处理成功 */
    public boolean processFileState(FileDetailEntity fileEntity) {
        if (fileEntity.getProcessingStatus() == null) {
            logger.warn("文件[{}]状态为空，设置为初始状态", fileEntity.getId());
            fileEntity.setProcessingStatus(FileProcessingStatusEnum.UPLOADED.getCode());
        }

        FileProcessingStateProcessor processor = processorMap.get(fileEntity.getProcessingStatus());
        if (processor == null) {
            logger.error("文件[{}]状态[{}]没有对应的处理器", fileEntity.getId(), fileEntity.getProcessingStatus());
            return false;
        }

        try {
            processor.process(fileEntity);
            return true;
        } catch (Exception e) {
            logger.error("文件[{}]状态处理失败", fileEntity.getId(), e);
            return false;
        }
    }

    /** 处理文件事件，进行状态转换
     * 
     * @param fileEntity 文件实体
     * @param event 处理事件
     * @return 是否转换成功 */
    public boolean handleEvent(FileDetailEntity fileEntity, FileProcessingEventEnum event) {
        Integer currentStatus = fileEntity.getProcessingStatus();
        if (currentStatus == null) {
            currentStatus = FileProcessingStatusEnum.UPLOADED.getCode();
            fileEntity.setProcessingStatus(currentStatus);
        }

        FileProcessingStatusEnum currentStatusEnum = FileProcessingStatusEnum.fromCode(currentStatus);
        Integer targetStatus = getTargetStatus(currentStatusEnum, event);

        if (targetStatus == null) {
            logger.warn("文件[{}]当前状态[{}]不支持事件[{}]", 
                    fileEntity.getId(), currentStatusEnum.getDescription(), event);
            return false;
        }

        if (!canTransition(currentStatus, targetStatus)) {
            logger.warn("文件[{}]不能从状态[{}]转换到状态[{}]", 
                    fileEntity.getId(), currentStatusEnum.getDescription(), 
                    FileProcessingStatusEnum.fromCode(targetStatus).getDescription());
            return false;
        }

        // 执行状态转换
        return transitionTo(fileEntity, targetStatus, event);
    }

    /** 根据当前状态和事件确定目标状态
     * 
     * @param currentStatus 当前状态
     * @param event 事件
     * @return 目标状态 */
    private Integer getTargetStatus(FileProcessingStatusEnum currentStatus, FileProcessingEventEnum event) {
        switch (event) {
            case START_OCR_PROCESSING:
                if (currentStatus == FileProcessingStatusEnum.UPLOADED || 
                    currentStatus == FileProcessingStatusEnum.OCR_FAILED) {
                    return FileProcessingStatusEnum.OCR_PROCESSING.getCode();
                }
                break;
            case COMPLETE_OCR_PROCESSING:
                if (currentStatus == FileProcessingStatusEnum.OCR_PROCESSING) {
                    return FileProcessingStatusEnum.OCR_COMPLETED.getCode();
                }
                break;
            case FAIL_OCR_PROCESSING:
                if (currentStatus == FileProcessingStatusEnum.OCR_PROCESSING) {
                    return FileProcessingStatusEnum.OCR_FAILED.getCode();
                }
                break;
            case START_EMBEDDING_PROCESSING:
                if (currentStatus == FileProcessingStatusEnum.OCR_COMPLETED || 
                    currentStatus == FileProcessingStatusEnum.EMBEDDING_FAILED) {
                    return FileProcessingStatusEnum.EMBEDDING_PROCESSING.getCode();
                }
                break;
            case COMPLETE_EMBEDDING_PROCESSING:
                if (currentStatus == FileProcessingStatusEnum.EMBEDDING_PROCESSING) {
                    return FileProcessingStatusEnum.COMPLETED.getCode();
                }
                break;
            case FAIL_EMBEDDING_PROCESSING:
                if (currentStatus == FileProcessingStatusEnum.EMBEDDING_PROCESSING) {
                    return FileProcessingStatusEnum.EMBEDDING_FAILED.getCode();
                }
                break;
            case RESET_PROCESSING:
                return FileProcessingStatusEnum.UPLOADED.getCode();
            case UPDATE_OCR_PROGRESS:
            case UPDATE_EMBEDDING_PROGRESS:
                return currentStatus.getCode(); // 进度更新不改变状态
            default:
                break;
        }
        return null;
    }

    /** 检查是否可以从当前状态转换到目标状态
     * 
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否可以转换 */
    private boolean canTransition(Integer currentStatus, Integer targetStatus) {
        if (currentStatus.equals(targetStatus)) {
            return true; // 状态相同，允许（用于进度更新等）
        }

        FileProcessingStateProcessor processor = processorMap.get(currentStatus);
        if (processor == null) {
            return false;
        }

        return processor.canTransitionTo(targetStatus);
    }

    /** 执行状态转换
     * 
     * @param fileEntity 文件实体
     * @param targetStatus 目标状态
     * @param event 触发事件
     * @return 是否转换成功 */
    private boolean transitionTo(FileDetailEntity fileEntity, Integer targetStatus, FileProcessingEventEnum event) {
        Integer oldStatus = fileEntity.getProcessingStatus();
        
        // 处理特殊事件
        if (event == FileProcessingEventEnum.UPDATE_OCR_PROGRESS) {
            // OCR进度更新不改变状态，只处理进度
            return processFileState(fileEntity);
        }
        
        if (event == FileProcessingEventEnum.UPDATE_EMBEDDING_PROGRESS) {
            // 向量化进度更新不改变状态，只处理进度
            return processFileState(fileEntity);
        }

        // 执行状态转换
        fileEntity.setProcessingStatus(targetStatus);
        
        // 根据事件执行特定操作
        executeEventActions(fileEntity, event);
        
        // 处理新状态
        boolean result = processFileState(fileEntity);
        
        if (result) {
            logger.info("文件[{}]状态转换成功: {} -> {} (事件: {})", 
                    fileEntity.getId(), 
                    FileProcessingStatusEnum.fromCode(oldStatus).getDescription(),
                    FileProcessingStatusEnum.fromCode(targetStatus).getDescription(),
                    event);
        } else {
            // 转换失败，回滚状态
            fileEntity.setProcessingStatus(oldStatus);
            logger.error("文件[{}]状态转换失败，已回滚", fileEntity.getId());
        }
        
        return result;
    }

    /** 执行事件相关的操作
     * 
     * @param fileEntity 文件实体
     * @param event 事件 */
    private void executeEventActions(FileDetailEntity fileEntity, FileProcessingEventEnum event) {
        switch (event) {
            case START_OCR_PROCESSING:
                fileEntity.setCurrentOcrPageNumber(0);
                fileEntity.setOcrProcessProgress(0.0);
                break;
            case COMPLETE_OCR_PROCESSING:
                fileEntity.setOcrProcessProgress(100.0);
                break;
            case START_EMBEDDING_PROCESSING:
                fileEntity.setCurrentEmbeddingPageNumber(0);
                fileEntity.setEmbeddingProcessProgress(0.0);
                break;
            case COMPLETE_EMBEDDING_PROCESSING:
                fileEntity.setEmbeddingProcessProgress(100.0);
                break;
            case RESET_PROCESSING:
                fileEntity.setCurrentOcrPageNumber(0);
                fileEntity.setCurrentEmbeddingPageNumber(0);
                fileEntity.setOcrProcessProgress(0.0);
                fileEntity.setEmbeddingProcessProgress(0.0);
                break;
            default:
                // 其他事件不需要特殊处理
                break;
        }
    }

    /** 更新OCR处理进度
     * 
     * @param fileEntity 文件实体
     * @param currentPage 当前页数
     * @param totalPages 总页数
     * @return 是否更新成功 */
    public boolean updateOcrProgress(FileDetailEntity fileEntity, Integer currentPage, Integer totalPages) {
        if (!FileProcessingStatusEnum.OCR_PROCESSING.getCode().equals(fileEntity.getProcessingStatus())) {
            logger.warn("文件[{}]当前状态不是OCR处理中，无法更新OCR进度", fileEntity.getId());
            return false;
        }

        fileEntity.setCurrentOcrPageNumber(currentPage);
        if (totalPages != null && totalPages > 0) {
            double progress = (double) currentPage / totalPages * 100;
            fileEntity.setOcrProcessProgress(Math.min(progress, 99.0)); // 最大99%，完成时通过事件设置为100%
        }

        return handleEvent(fileEntity, FileProcessingEventEnum.UPDATE_OCR_PROGRESS);
    }

    /** 更新向量化处理进度
     * 
     * @param fileEntity 文件实体
     * @param currentPage 当前页数
     * @param totalPages 总页数
     * @return 是否更新成功 */
    public boolean updateEmbeddingProgress(FileDetailEntity fileEntity, Integer currentPage, Integer totalPages) {
        if (!FileProcessingStatusEnum.EMBEDDING_PROCESSING.getCode().equals(fileEntity.getProcessingStatus())) {
            logger.warn("文件[{}]当前状态不是向量化处理中，无法更新向量化进度", fileEntity.getId());
            return false;
        }

        fileEntity.setCurrentEmbeddingPageNumber(currentPage);
        if (totalPages != null && totalPages > 0) {
            double progress = (double) currentPage / totalPages * 100;
            fileEntity.setEmbeddingProcessProgress(Math.min(progress, 99.0)); // 最大99%，完成时通过事件设置为100%
        }

        return handleEvent(fileEntity, FileProcessingEventEnum.UPDATE_EMBEDDING_PROGRESS);
    }
}