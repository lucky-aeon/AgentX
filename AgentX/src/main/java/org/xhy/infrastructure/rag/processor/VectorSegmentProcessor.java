package org.xhy.infrastructure.rag.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.message.RagDocSyncStorageMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.service.FileDetailDomainService;
import org.xhy.domain.rag.straegy.context.ProcessingContext;
import org.xhy.infrastructure.mq.enums.EventType;
import org.xhy.infrastructure.mq.events.RagDocSyncStorageEvent;
import org.xhy.infrastructure.rag.service.UserModelConfigResolver;

import java.util.List;

/**
 * 向量段落处理器
 * 
 * 负责翻译+分割+向量化的完整处理链：
 * 1. 读取DocumentUnitEntity原文
 * 2. 翻译特殊节点（内存处理）
 * 3. 检查翻译后长度
 * 4. 如超限则二次分割
 * 5. 触发向量化处理
 * 
 * @author claude
 */
@Service
public class VectorSegmentProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(VectorSegmentProcessor.class);
    
    private final SpecialNodeTranslator translator;
    private final SecondarySegmentSplitter splitter;
    private final DocumentUnitRepository documentUnitRepository;
    private final ApplicationContext applicationContext;
    private final FileDetailDomainService fileDetailDomainService;
    private final UserModelConfigResolver userModelConfigResolver;
    
    public VectorSegmentProcessor(SpecialNodeTranslator translator,
                                 SecondarySegmentSplitter splitter,
                                 DocumentUnitRepository documentUnitRepository,
                                 ApplicationContext applicationContext,
                                 FileDetailDomainService fileDetailDomainService,
                                 UserModelConfigResolver userModelConfigResolver) {
        this.translator = translator;
        this.splitter = splitter;
        this.documentUnitRepository = documentUnitRepository;
        this.applicationContext = applicationContext;
        this.fileDetailDomainService = fileDetailDomainService;
        this.userModelConfigResolver = userModelConfigResolver;
    }
    
    /**
     * 批量处理文档单元
     * 
     * @param units 文档单元列表
     * @param context 处理上下文
     */
    public void processDocumentUnits(List<DocumentUnitEntity> units, ProcessingContext context) {
        if (units == null || units.isEmpty()) {
            log.debug("No document units to process");
            return;
        }
        
        log.info("Starting vector segment processing for {} document units", units.size());
        
        int successCount = 0;
        int errorCount = 0;
        
        for (DocumentUnitEntity unit : units) {
            try {
                processSingleUnit(unit, context);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to process document unit {}: {}", unit.getId(), e.getMessage(), e);
                errorCount++;
            }
        }
        
        log.info("Vector segment processing completed. Success: {}, Error: {}", successCount, errorCount);
    }
    
    /**
     * 处理单个文档单元
     * 
     * @param unit 文档单元
     * @param context 处理上下文
     */
    public void processSingleUnit(DocumentUnitEntity unit, ProcessingContext context) {
        if (unit == null || unit.getContent() == null) {
            log.warn("Document unit or content is null, skipping");
            return;
        }
        
        String unitId = unit.getId();
        String originalContent = unit.getContent();
        
        log.debug("Processing document unit {}: content length = {}", unitId, originalContent.length());
        
        try {
            // 第1步：翻译特殊节点（内存中处理）
            String translatedContent = translator.translateSpecialNodes(originalContent, context);
            
            if (translatedContent.equals(originalContent)) {
                log.debug("No special nodes translated for unit {}", unitId);
            } else {
                log.debug("Special nodes translated for unit {}: {} -> {} chars", 
                        unitId, originalContent.length(), translatedContent.length());
            }
            
            // 第2步：检查长度并进行二次分割
            String titleContext = extractTitleContext(unit);
            List<String> vectorTexts = splitter.splitIfNeeded(translatedContent, titleContext);
            
            log.debug("Split result for unit {}: {} chunks. {}", 
                    unitId, vectorTexts.size(), 
                    splitter.getSplitStatistics(translatedContent, vectorTexts));
            
            // 第3步：为每个分割片段创建DocumentUnitEntity并触发向量化
            createVectorSegments(unit, vectorTexts, context);
            
        } catch (Exception e) {
            log.error("Error processing document unit {}: {}", unitId, e.getMessage(), e);
            throw new RuntimeException("Failed to process document unit " + unitId, e);
        }
    }
    
    /**
     * 为分割片段创建DocumentUnitEntity并触发向量化
     */
    private void createVectorSegments(DocumentUnitEntity originalUnit, List<String> vectorTexts, ProcessingContext context) {
        String originalUnitId = originalUnit.getId();
        
        if (vectorTexts.size() == 1) {
            // 无需分割，直接更新原记录
            String vectorText = vectorTexts.get(0);
            updateOriginalUnit(originalUnit, vectorText);
            triggerVectorization(originalUnit, context);
            
            log.debug("Updated original unit {} with translated content", originalUnitId);
            
        } else {
            // 需要分割，创建多个记录
            log.info("Splitting unit {} into {} vector segments", originalUnitId, vectorTexts.size());
            
            for (int i = 0; i < vectorTexts.size(); i++) {
                String vectorText = vectorTexts.get(i);
                
                if (i == 0) {
                    // 第一个片段更新原记录
                    updateOriginalUnit(originalUnit, vectorText);
                    triggerVectorization(originalUnit, context);
                    
                } else {
                    // 其他片段创建新记录
                    DocumentUnitEntity newUnit = createNewVectorUnit(originalUnit, vectorText, i);
                    documentUnitRepository.insert(newUnit);
                    triggerVectorization(newUnit, context);
                    
                    log.debug("Created new vector unit {} from original unit {}", 
                            newUnit.getId(), originalUnitId);
                }
            }
        }
    }
    
    /**
     * 更新原始单元的内容
     */
    private void updateOriginalUnit(DocumentUnitEntity unit, String vectorText) {
        unit.setContent(vectorText);
        unit.setIsVector(false); // 重置向量化状态，等待重新向量化
        documentUnitRepository.updateById(unit);
    }
    
    /**
     * 创建新的向量单元
     */
    private DocumentUnitEntity createNewVectorUnit(DocumentUnitEntity originalUnit, String vectorText, int segmentIndex) {
        DocumentUnitEntity newUnit = new DocumentUnitEntity();
        newUnit.setContent(vectorText);
        newUnit.setFileId(originalUnit.getFileId());
        
        // 生成新的页码，避免冲突
        // 使用原页码 * 1000 + 段落索引的方式
        int newPage = originalUnit.getPage() * 1000 + segmentIndex;
        newUnit.setPage(newPage);
        
        newUnit.setIsVector(false); // 新记录需要向量化
        newUnit.setIsOcr(true);     // 标记为已处理
        
        return newUnit;
    }
    
    /**
     * 触发向量化处理
     */
    private void triggerVectorization(DocumentUnitEntity unit, ProcessingContext context) {
        try {
            // 获取文件详情来构建完整的向量化消息
            FileDetailEntity fileEntity = fileDetailDomainService.getFileByIdWithoutUserCheck(unit.getFileId());
            
            // 构建完整的RagDocSyncStorageMessage
            RagDocSyncStorageMessage storageMessage = new RagDocSyncStorageMessage();
            storageMessage.setId(unit.getId());
            storageMessage.setFileId(unit.getFileId());
            storageMessage.setFileName(fileEntity.getOriginalFilename()); // 关键：设置文件名
            storageMessage.setPage(unit.getPage());
            storageMessage.setContent(unit.getContent());
            storageMessage.setVector(false); // 待向量化
            storageMessage.setUserId(context.getUserId());
            storageMessage.setDatasetId(fileEntity.getDataSetId());
            
            // 设置嵌入模型配置
            try {
                storageMessage.setEmbeddingModelConfig(
                    userModelConfigResolver.getUserEmbeddingModelConfig(context.getUserId())
                );
            } catch (Exception e) {
                log.warn("Failed to get embedding model config for user {}: {}", 
                        context.getUserId(), e.getMessage());
                // 继续处理，让后续流程处理模型配置问题
            }
            
            // 发送向量化消息到消息队列
            RagDocSyncStorageEvent<RagDocSyncStorageMessage> storageEvent = 
                new RagDocSyncStorageEvent<>(storageMessage, EventType.DOC_SYNC_RAG);
            storageEvent.setDescription("二次分割后的向量化处理任务 - 页面 " + unit.getPage());
            
            applicationContext.publishEvent(storageEvent);
            
            log.debug("Triggered vectorization for unit {} with fileName: {}", 
                    unit.getId(), fileEntity.getOriginalFilename());
            
        } catch (Exception e) {
            log.error("Failed to trigger vectorization for unit {}: {}", 
                    unit.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to trigger vectorization", e);
        }
    }
    
    /**
     * 提取标题上下文
     * 
     * 从原文中提取标题信息，用于在分割时保持上下文
     */
    private String extractTitleContext(DocumentUnitEntity unit) {
        String content = unit.getContent();
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        // 查找第一行是否为标题
        String[] lines = content.split("\n", 2);
        String firstLine = lines[0].trim();
        
        if (firstLine.startsWith("#")) {
            // 是markdown标题
            return firstLine;
        }
        
        // 如果内容很短，可能整体就是标题上下文
        if (content.length() < 100) {
            return null; // 内容太短，无需额外上下文
        }
        
        // 查找多级标题结构
        StringBuilder titlePath = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                if (titlePath.length() > 0) {
                    titlePath.append(" > ");
                }
                titlePath.append(trimmed);
            } else if (trimmed.isEmpty()) {
                continue; // 跳过空行
            } else {
                break; // 遇到内容行，停止提取标题
            }
        }
        
        return titlePath.length() > 0 ? titlePath.toString() : null;
    }
    
    /**
     * 批量处理（异步调用入口）
     * 
     * @param units 文档单元列表
     */
    public void processDocumentUnitsAsync(List<DocumentUnitEntity> units) {
        if (units == null || units.isEmpty()) {
            return;
        }
        
        log.info("Starting async vector segment processing for {} units", units.size());
        
        try {
            // 创建默认的处理上下文
            // 注意：异步处理时可能没有完整的用户上下文，使用基础配置
            ProcessingContext context = createDefaultProcessingContext(units.get(0));
            
            processDocumentUnits(units, context);
            
        } catch (Exception e) {
            log.error("Async vector segment processing failed", e);
            throw e;
        }
    }
    
    /**
     * 创建默认的处理上下文
     */
    private ProcessingContext createDefaultProcessingContext(DocumentUnitEntity sampleUnit) {
        try {
            // 从文件信息中获取用户ID
            FileDetailEntity fileEntity = fileDetailDomainService.getFileByIdWithoutUserCheck(sampleUnit.getFileId());
            String userId = fileEntity.getUserId();
            String fileId = sampleUnit.getFileId();
            
            // 尝试获取用户模型配置
            try {
                return ProcessingContext.from(createRagDocSyncOcrMessage(fileEntity), userModelConfigResolver);
            } catch (Exception e) {
                log.warn("Failed to create full processing context, using simplified context: {}", e.getMessage());
                // 回退到简化的上下文
                return new ProcessingContext(null, null, null, userId, fileId);
            }
            
        } catch (Exception e) {
            log.error("Failed to create processing context: {}", e.getMessage(), e);
            // 最终回退：使用系统用户
            return new ProcessingContext(null, null, null, "system", sampleUnit.getFileId());
        }
    }
    
    /**
     * 从FileDetailEntity创建RagDocSyncOcrMessage（用于构建ProcessingContext）
     */
    private RagDocSyncOcrMessage createRagDocSyncOcrMessage(FileDetailEntity fileEntity) {
        RagDocSyncOcrMessage message = new RagDocSyncOcrMessage();
        message.setFileId(fileEntity.getId());
        message.setUserId(fileEntity.getUserId());
        // 其他字段可以为null，ProcessingContext.from会处理
        return message;
    }
    
    /**
     * 获取处理统计信息
     */
    public ProcessingStatistics getProcessingStatistics(List<DocumentUnitEntity> units) {
        if (units == null || units.isEmpty()) {
            return new ProcessingStatistics(0, 0, 0, 0);
        }
        
        int totalUnits = units.size();
        int totalContentLength = units.stream()
                .mapToInt(unit -> unit.getContent() != null ? unit.getContent().length() : 0)
                .sum();
        
        long vectorizedUnits = units.stream()
                .mapToLong(unit -> unit.getIsVector() != null && unit.getIsVector() ? 1 : 0)
                .sum();
        
        int avgContentLength = totalContentLength / Math.max(totalUnits, 1);
        
        return new ProcessingStatistics(totalUnits, totalContentLength, (int) vectorizedUnits, avgContentLength);
    }
    
    /**
     * 处理统计信息
     */
    public static class ProcessingStatistics {
        private final int totalUnits;
        private final int totalContentLength;
        private final int vectorizedUnits;
        private final int avgContentLength;
        
        public ProcessingStatistics(int totalUnits, int totalContentLength, 
                                  int vectorizedUnits, int avgContentLength) {
            this.totalUnits = totalUnits;
            this.totalContentLength = totalContentLength;
            this.vectorizedUnits = vectorizedUnits;
            this.avgContentLength = avgContentLength;
        }
        
        public int getTotalUnits() { return totalUnits; }
        public int getTotalContentLength() { return totalContentLength; }
        public int getVectorizedUnits() { return vectorizedUnits; }
        public int getAvgContentLength() { return avgContentLength; }
        public double getVectorizedRatio() { 
            return totalUnits > 0 ? (double) vectorizedUnits / totalUnits : 0.0; 
        }
        
        @Override
        public String toString() {
            return String.format("ProcessingStatistics{totalUnits=%d, totalContentLength=%d, " +
                    "vectorizedUnits=%d, avgContentLength=%d, vectorizedRatio=%.2f%%}", 
                    totalUnits, totalContentLength, vectorizedUnits, avgContentLength, 
                    getVectorizedRatio() * 100);
        }
    }
}