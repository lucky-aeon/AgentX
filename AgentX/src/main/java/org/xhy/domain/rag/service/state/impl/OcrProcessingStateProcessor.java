package org.xhy.domain.rag.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.constant.FileProcessingStatusEnum;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.service.state.FileProcessingStateProcessor;

/** OCR处理中状态处理器
 * 
 * @author zang
 * @date 2025-07-17 */
@Component
public class OcrProcessingStateProcessor implements FileProcessingStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OcrProcessingStateProcessor.class);

    @Override
    public Integer getStatus() {
        return FileProcessingStatusEnum.OCR_PROCESSING.getCode();
    }

    @Override
    public void process(FileDetailEntity fileEntity) {
        logger.info("文件[{}]正在进行OCR处理，当前进度: {}%", fileEntity.getId(), fileEntity.getOcrProcessProgress());

        // 可以在这里添加OCR处理的业务逻辑，比如：
        // 1. 检查OCR任务状态
        // 2. 更新处理进度
        // 3. 处理异常情况
    }

    @Override
    public Integer[] getNextPossibleStatuses() {
        return new Integer[]{FileProcessingStatusEnum.OCR_COMPLETED.getCode(),
                FileProcessingStatusEnum.OCR_FAILED.getCode()};
    }
}