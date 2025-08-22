package org.xhy.domain.rag.strategy;

import org.xhy.domain.rag.message.RagDocMessage;

/** @author shilong.zang
 * @date 09:54 <br/>
 */
public interface DocumentProcessingStrategy {

    /** 处理
     * @param ragDocSyncOcrMessage mq消息
     * @param strategy 策略 */
    void handle(RagDocMessage ragDocSyncOcrMessage, String strategy) throws Exception;

}
