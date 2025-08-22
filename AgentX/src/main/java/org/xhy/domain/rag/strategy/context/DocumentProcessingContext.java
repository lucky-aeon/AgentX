package org.xhy.domain.rag.strategy.context;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.xhy.domain.rag.model.enums.DocumentProcessingType;
import org.xhy.domain.rag.strategy.DocumentProcessingStrategy;

import jakarta.annotation.Resource;

/** @author shilong.zang
 * @date 09:39 <br/>
 */
@Service
public class DocumentProcessingContext {

    @Resource
    private Map<String, DocumentProcessingStrategy> documentProcessingStrategyMap;

    public DocumentProcessingStrategy getDocumentStrategyHandler(String strategy) {
        return documentProcessingStrategyMap.get(DocumentProcessingType.getLabelByValue(strategy));
    }
}
