package org.xhy.domain.rag.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.constant.FileProcessingStatusEnum;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.service.state.FileProcessingStateProcessor;

/** 处理完成状态处理器
 * 
 * @author zang
 * @date 2025-07-17 */
@Component
public class CompletedStateProcessor implements FileProcessingStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CompletedStateProcessor.class);

    @Override
    public Integer getStatus() {
        return FileProcessingStatusEnum.COMPLETED.getCode();
    }

    @Override
    public void process(FileDetailEntity fileEntity) {
        logger.info("文件[{}]处理已全部完成", fileEntity.getId());

        // 确保所有进度都是100%
        fileEntity.setOcrProcessProgress(100.0);
        fileEntity.setEmbeddingProcessProgress(100.0);

        // 可以在这里添加完成后的后置处理，比如：
        // 1. 发送通知
        // 2. 清理临时文件
        // 3. 更新统计信息
    }

    @Override
    public Integer[] getNextPossibleStatuses() {
        // 完成状态一般不再转换到其他状态，除非重置
        return new Integer[]{FileProcessingStatusEnum.UPLOADED.getCode() // 只允许重置
        };
    }
}