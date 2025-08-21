package org.xhy.domain.rag.strategy;

import org.xhy.domain.rag.message.RagDocSyncOcrMessage;

/** @author shilong.zang
 * @date 09:54 <br/>
 */
public interface RagDocSyncOcrStrategy {

    /** 处理
     * @param ragDocSyncOcrMessage mq消息
     * @param strategy 策略 */
    void handle(RagDocSyncOcrMessage ragDocSyncOcrMessage, String strategy) throws Exception;

}
