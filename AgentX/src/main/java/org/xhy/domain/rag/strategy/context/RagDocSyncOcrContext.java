package org.xhy.domain.rag.strategy.context;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.xhy.domain.rag.model.enums.DocumentProcessingType;
import org.xhy.domain.rag.strategy.RagDocSyncOcrStrategy;

import jakarta.annotation.Resource;

/** @author shilong.zang
 * @date 09:39 <br/>
 */
@Service
public class RagDocSyncOcrContext {

    @Resource
    private Map<String, RagDocSyncOcrStrategy> taskExportStrategyMap;

    public RagDocSyncOcrStrategy getTaskExportStrategy(String strategy) {
        return taskExportStrategyMap.get(DocumentProcessingType.getLabelByValue(strategy));
    }
}
