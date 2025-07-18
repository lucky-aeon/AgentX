package org.xhy.domain.rag.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.constant.FileProcessingStatusEnum;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.service.state.FileProcessingStateProcessor;

/** 向量化处理失败状态处理器
 * 
 * @author zang
 * @date 2025-07-17 */
@Component
public class EmbeddingFailedStateProcessor implements FileProcessingStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingFailedStateProcessor.class);

    @Override
    public Integer getStatus() {
        return FileProcessingStatusEnum.EMBEDDING_FAILED.getCode();
    }

    @Override
    public void process(FileDetailEntity fileEntity) {
        logger.warn("文件[{}]向量化处理失败", fileEntity.getId());
        
        // 可以在这里添加失败处理逻辑，比如：
        // 1. 记录失败原因
        // 2. 发送失败通知
        // 3. 清理临时文件
        // 4. 准备重试
    }

    @Override
    public Integer[] getNextPossibleStatuses() {
        return new Integer[]{
            FileProcessingStatusEnum.EMBEDDING_PROCESSING.getCode(), // 允许重试
            FileProcessingStatusEnum.OCR_COMPLETED.getCode(),        // 回退到OCR完成状态
            FileProcessingStatusEnum.UPLOADED.getCode()              // 允许完全重置
        };
    }
}