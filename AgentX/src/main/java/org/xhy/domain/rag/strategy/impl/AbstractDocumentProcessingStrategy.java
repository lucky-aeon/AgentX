package org.xhy.domain.rag.strategy.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.rag.message.RagDocMessage;
import org.xhy.domain.rag.strategy.DocumentProcessingStrategy;

public abstract class AbstractDocumentProcessingStrategy implements DocumentProcessingStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDocumentProcessingStrategy.class);

    /** 处理消息
     * @param ragDocMessage 消息数据
     * @param strategy 当前策略 */
    @Override
    public void handle(RagDocMessage ragDocMessage, String strategy) throws Exception {

        final byte[] fileData = getFileData(ragDocMessage, strategy);
        pushPageSize(fileData, ragDocMessage);
        if (fileData == null) {
            LOG.error("文件数据为空");
            return;
        }

        Integer pageSize = ragDocMessage.getPageSize();
        if (pageSize == null) {
            LOG.warn("页面大小为空，使用默认值1用于txt/word文件");
            pageSize = 1;
        }
        final Map<Integer, String> data = processFile(fileData, pageSize, ragDocMessage);

        LOG.info("成功从当前文件获取 {} 页数据", data.size());

        insertData(ragDocMessage, data);

    };

    /** 获取文件页数 */
    abstract public void pushPageSize(byte[] bytes, RagDocMessage ragDocSyncOcrMessage);

    /** 获取文件
     * @param ragDocSyncOcrMessage 消息数据
     * @param strategy 当前策略 */
    abstract public byte[] getFileData(RagDocMessage ragDocSyncOcrMessage, String strategy);

    /** ocr数据 */
    abstract public Map<Integer, String> processFile(byte[] fileBytes, int totalPages);

    /** ocr数据 (带消息参数，子类可选择性重写此方法) */
    public Map<Integer, String> processFile(byte[] fileBytes, int totalPages, RagDocMessage ragDocSyncOcrMessage) {
        return processFile(fileBytes, totalPages);
    }

    /** 保存数据 */
    abstract public void insertData(RagDocMessage ragDocSyncOcrMessage, Map<Integer, String> ocrData) throws Exception;
}
