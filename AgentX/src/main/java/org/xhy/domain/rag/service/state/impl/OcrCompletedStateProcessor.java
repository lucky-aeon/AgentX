package org.xhy.domain.rag.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.constant.FileProcessingStatusEnum;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.service.state.FileProcessingStateProcessor;

/** OCR处理完成状态处理器
 * 
 * @author zang
 * @date 2025-07-17 */
@Component
public class OcrCompletedStateProcessor implements FileProcessingStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OcrCompletedStateProcessor.class);

    @Override
    public Integer getStatus() {
        return FileProcessingStatusEnum.OCR_COMPLETED.getCode();
    }

    @Override
    public void process(FileDetailEntity fileEntity) {
        logger.info("文件[{}]OCR处理已完成，等待开始向量化处理", fileEntity.getId());

        // 确保OCR进度为100%
        fileEntity.setOcrProcessProgress(100.0);

        // 初始化向量化相关字段
        if (fileEntity.getCurrentEmbeddingPageNumber() == null) {
            fileEntity.setCurrentEmbeddingPageNumber(0);
        }
        if (fileEntity.getEmbeddingProcessProgress() == null) {
            fileEntity.setEmbeddingProcessProgress(0.0);
        }
    }

    @Override
    public Integer[] getNextPossibleStatuses() {
        return new Integer[]{FileProcessingStatusEnum.EMBEDDING_PROCESSING.getCode()};
    }
}