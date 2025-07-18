package org.xhy.domain.rag.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.constant.FileProcessingStatusEnum;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.service.state.FileProcessingStateProcessor;

/** 已上传状态处理器
 * 
 * @author zang
 * @date 2025-07-17 */
@Component
public class UploadedStateProcessor implements FileProcessingStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(UploadedStateProcessor.class);

    @Override
    public Integer getStatus() {
        return FileProcessingStatusEnum.UPLOADED.getCode();
    }

    @Override
    public void process(FileDetailEntity fileEntity) {
        logger.info("文件[{}]处于已上传状态，等待开始OCR处理", fileEntity.getId());
        
        // 初始化进度信息
        if (fileEntity.getCurrentOcrPageNumber() == null) {
            fileEntity.setCurrentOcrPageNumber(0);
        }
        if (fileEntity.getOcrProcessProgress() == null) {
            fileEntity.setOcrProcessProgress(0.0);
        }
        if (fileEntity.getCurrentEmbeddingPageNumber() == null) {
            fileEntity.setCurrentEmbeddingPageNumber(0);
        }
        if (fileEntity.getEmbeddingProcessProgress() == null) {
            fileEntity.setEmbeddingProcessProgress(0.0);
        }
    }

    @Override
    public Integer[] getNextPossibleStatuses() {
        return new Integer[]{
            FileProcessingStatusEnum.OCR_PROCESSING.getCode()
        };
    }
}