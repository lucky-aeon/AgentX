# RAG二次分割架构设计文档

## 1. 问题背景

### 1.1 当前问题分析

在现有的RAG（Retrieval-Augmented Generation）系统中，我们面临以下核心问题：

#### 问题一：长度计算不准确
- **现状**：分段时基于占位符长度（约25字符）进行计算
- **实际情况**：特殊节点（代码块、表格、图片）翻译后可能达到几千字符
- **结果**：预估1800字符的段落，实际使用时可能超过5000字符，导致token限制问题

#### 问题二：内容版本管理混乱
- **向量化内容**：应该使用什么版本？占位符还是翻译后内容？
- **存储显示**：应该保存哪个版本？
- **RAG使用**：getFinalContent()返回原始内容，可能超出预期长度

### 1.2 根本原因
当前架构试图在不知道翻译结果的情况下预测翻译后的长度，这在技术上是不可能准确的。任何预测都会影响翻译质量或导致长度限制问题。

## 2. 解决方案概述

### 2.1 核心思想
**原文归原文，向量归向量** - 彻底分离原文存储和向量化处理

### 2.2 解决策略
采用**延迟分段策略**：
1. 先保证内容质量（无长度限制翻译）
2. 再根据实际结果进行分段（基于真实长度）
3. 分离存储（原文完整保存，向量智能分段）

## 3. 架构设计

### 3.1 整体架构

```
原始Markdown文档
        ↓
   语义结构解析 (StructuralMarkdownProcessor)
        ↓
   DocumentUnitEntity (原文存储层)
        ↓
   VectorSegmentProcessor (翻译+分割处理)
        ↓
   DocumentChunkEntity (向量存储层, 1:N)
```

### 3.2 数据流设计

#### 阶段1：纯语义拆分
```
输入：原始Markdown
处理：StructuralMarkdownProcessor（纯拆分模式）
输出：DocumentUnitEntity[]（存储纯原文）
特点：不处理特殊节点，保持原始格式
```

#### 阶段2：内存翻译处理
```
输入：DocumentUnitEntity.content（原文）
处理：特殊节点翻译（内存中进行）
输出：翻译后文本（不写回数据库）
特点：高质量翻译，无长度限制
```

#### 阶段3：智能二次分割
```
输入：翻译后文本
处理：基于实际长度的智能分割
输出：多个向量文本片段
特点：保持语义完整性，避免断句
```

#### 阶段4：向量化存储
```
输入：向量文本片段[]
处理：向量化处理
输出：DocumentChunkEntity[]（1:N关系）
特点：包含回溯到原文的元数据
```

### 3.3 存储架构

#### DocumentUnitEntity（原文存储）
```java
{
    id: "du_123456",
    fileId: "file_789",
    page: 1,
    content: "# 原始标题\n原始内容...\n```java\n原始代码\n```",  // 纯原文
    isVector: false,  // 标记是否已处理向量化
    isOcr: true
}
```

#### vector_store（向量存储）
```java
{
    embedding_id: "chunk_001",
    text: "用户管理 > 计费功能 > 包月计费\n\n这是代码示例的详细说明...",  // 翻译后的可搜索文本（包含标题上下文）
    embedding: [0.1, 0.2, ...],  // 1024维向量
    metadata: {
        "DATA_SET_ID": "a8d09a8821a3157fc74e3e7ddf74fc68",
        "DOCUMENT_ID": "du_123456",  // 关联DocumentUnitEntity
        "FILE_ID": "file_789",
        "FILE_NAME": "计费.md"
    }
}
```

## 4. 实现方案

### 4.1 核心组件设计

#### VectorSegmentProcessor
主要负责翻译+分割+向量化的完整处理链

```java
@Service
public class VectorSegmentProcessor {
    
    private final SpecialNodeTranslator translator;
    private final SecondarySegmentSplitter splitter;
    private final VectorEmbeddingService vectorService;
    
    public void processDocumentUnits(List<DocumentUnitEntity> units) {
        for (DocumentUnitEntity unit : units) {
            processSingleUnit(unit);
        }
    }
    
    private void processSingleUnit(DocumentUnitEntity unit) {
        // 1. 翻译特殊节点（内存中）
        String translatedContent = translator.translateSpecialNodes(unit.getContent());
        
        // 2. 检查长度并分割
        List<String> vectorTexts = splitter.splitIfNeeded(translatedContent);
        
        // 3. 为每个分割片段创建新的DocumentUnitEntity并触发向量化
        for (int i = 0; i < vectorTexts.size(); i++) {
            if (i == 0) {
                // 第一个片段更新原记录
                unit.setContent(vectorTexts.get(i));
                documentUnitRepository.updateById(unit);
                
                // 发送向量化消息
                applicationContext.publishEvent(
                    new RagDocSyncStorageEvent<>(unit, EventType.DOC_SYNC_RAG)
                );
            } else {
                // 其他片段创建新记录
                DocumentUnitEntity newUnit = new DocumentUnitEntity();
                newUnit.setContent(vectorTexts.get(i));
                newUnit.setFileId(unit.getFileId());
                newUnit.setPage(unit.getPage() * 1000 + i);  // 避免页码冲突
                newUnit.setIsVector(false);
                newUnit.setIsOcr(true);
                
                documentUnitRepository.insert(newUnit);
                
                // 发送向量化消息
                applicationContext.publishEvent(
                    new RagDocSyncStorageEvent<>(newUnit, EventType.DOC_SYNC_RAG)
                );
            }
        }
    }
}
```

#### SpecialNodeTranslator
专门处理特殊节点翻译

```java
@Component
public class SpecialNodeTranslator {
    
    private final List<SegmentEnhancer> enhancers;
    
    public String translateSpecialNodes(String originalContent) {
        // 解析内容，识别特殊节点
        List<SpecialNode> specialNodes = parseSpecialNodes(originalContent);
        
        if (specialNodes.isEmpty()) {
            return originalContent;  // 无特殊节点，返回原文
        }
        
        String result = originalContent;
        
        // 翻译每个特殊节点
        for (SpecialNode node : specialNodes) {
            String translatedNode = translateNode(node);
            result = result.replace(node.getOriginalContent(), translatedNode);
        }
        
        return result;
    }
    
    private String translateNode(SpecialNode node) {
        for (SegmentEnhancer enhancer : enhancers) {
            if (enhancer.canEnhance(node.getType())) {
                return enhancer.enhance(node);
            }
        }
        return node.getOriginalContent();  // 无法翻译则保持原样
    }
}
```

#### SecondarySegmentSplitter
智能二次分割器

```java
@Component
public class SecondarySegmentSplitter {
    
    @Value("${rag.vector.max-length:1800}")
    private int maxVectorLength;
    
    @Value("${rag.vector.overlap-size:100}")
    private int overlapSize;
    
    public List<String> splitIfNeeded(String content) {
        if (content.length() <= maxVectorLength) {
            return List.of(content);
        }
        
        return performSmartSplit(content);
    }
    
    private List<String> performSmartSplit(String content) {
        List<String> chunks = new ArrayList<>();
        
        // 优先在段落边界分割
        String[] paragraphs = content.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() <= maxVectorLength) {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            } else {
                // 当前段落已满，保存并开始新段落
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk.setLength(0);
                }
                
                // 处理超长单段落
                if (paragraph.length() > maxVectorLength) {
                    chunks.addAll(splitLongParagraph(paragraph));
                } else {
                    currentChunk.append(paragraph);
                }
            }
        }
        
        // 添加最后一个片段
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
    
    private List<String> splitLongParagraph(String paragraph) {
        // 在句子边界分割
        List<String> sentences = splitToSentences(paragraph);
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() <= maxVectorLength) {
                currentChunk.append(sentence);
            } else {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk.setLength(0);
                }
                
                // 如果单个句子仍然超长，强制截断
                if (sentence.length() > maxVectorLength) {
                    chunks.addAll(forceChunk(sentence));
                } else {
                    currentChunk.append(sentence);
                }
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
}
```

### 4.2 修改现有组件

#### StructuralMarkdownProcessor修改
增加纯原文拆分模式

```java
@Component("structuralMarkdownProcessor")
public class StructuralMarkdownProcessor implements MarkdownProcessor {
    
    // 新增：纯原文拆分模式配置
    @Value("${rag.processor.raw-mode:false}")
    private boolean rawMode;
    
    @Override
    public List<ProcessedSegment> processToSegments(String markdown, ProcessingContext context) {
        if (rawMode) {
            return processRawSegments(markdown, context);
        } else {
            return processWithPlaceholders(markdown, context);
        }
    }
    
    /**
     * 纯原文拆分模式 - 不使用占位符，保持原始格式
     */
    private List<ProcessedSegment> processRawSegments(String markdown, ProcessingContext context) {
        // 解析Markdown为AST
        Node document = parser.parse(markdown);
        
        // 构建文档树，但不使用占位符
        DocumentTree documentTree = buildRawDocumentTree(document);
        
        // 执行基于真实内容长度的分割
        return documentTree.performRawHierarchicalSplit();
    }
    
    /**
     * 构建保持原始内容的文档树
     */
    private DocumentTree buildRawDocumentTree(Node document) {
        DocumentTree tree = new DocumentTree(markdownProperties.getSegmentSplit());
        
        Stack<HeadingNode> nodeStack = new Stack<>();
        HeadingNode currentHeading = null;
        
        for (Node child : document.getChildren()) {
            if (child instanceof Heading) {
                // 处理标题节点
                Heading heading = (Heading) child;
                String headingText = extractTextContent(heading);
                HeadingNode newNode = new HeadingNode(heading.getLevel(), headingText);
                
                // 维护层级关系
                while (!nodeStack.isEmpty() && nodeStack.peek().getLevel() >= heading.getLevel()) {
                    nodeStack.pop();
                }
                
                if (nodeStack.isEmpty()) {
                    tree.addRootNode(newNode);
                } else {
                    nodeStack.peek().addChild(newNode);
                }
                
                nodeStack.push(newNode);
                currentHeading = newNode;
                
            } else {
                // 处理内容节点 - 保持原始格式
                if (currentHeading != null) {
                    String nodeContent = extractRawContent(child);  // 新方法：提取原始内容
                    if (!nodeContent.trim().isEmpty()) {
                        currentHeading.addDirectContent(nodeContent);
                    }
                } else {
                    // 创建虚拟根节点
                    if (tree.getRootNodes().isEmpty()) {
                        HeadingNode virtualRoot = new HeadingNode(1, "文档内容");
                        tree.addRootNode(virtualRoot);
                        currentHeading = virtualRoot;
                        nodeStack.push(virtualRoot);
                    }
                    
                    String nodeContent = extractRawContent(child);
                    if (!nodeContent.trim().isEmpty()) {
                        currentHeading.addDirectContent(nodeContent);
                    }
                }
            }
        }
        
        return tree;
    }
    
    /**
     * 提取节点的原始内容（不使用占位符）
     */
    private String extractRawContent(Node node) {
        // 直接返回节点的原始markdown内容
        return node.getChars().toString();
    }
}
```

#### MarkdownRagDocSyncOcrStrategyImpl修改
调整处理流程以支持两阶段处理

```java
@Service("ragDocSyncOcr-MARKDOWN")
public class MarkdownRagDocSyncOcrStrategyImpl extends RagDocSyncOcrStrategyImpl {
    
    private final VectorSegmentProcessor vectorProcessor;
    
    @Override
    public Map<Integer, String> processFile(byte[] fileBytes, int totalPages, RagDocSyncOcrMessage ragDocSyncOcrMessage) {
        
        String markdown = new String(fileBytes, StandardCharsets.UTF_8);
        
        // 第一阶段：纯原文拆分
        List<ProcessedSegment> rawSegments = processRawSegments(markdown, ragDocSyncOcrMessage);
        
        Map<Integer, String> ocrData = new HashMap<>();
        
        // 存储原文到DocumentUnitEntity
        for (int i = 0; i < rawSegments.size(); i++) {
            ProcessedSegment segment = rawSegments.get(i);
            ocrData.put(i, segment.getContent());  // 存储纯原文
        }
        
        return ocrData;
    }
    
    @Override
    public void insertData(RagDocSyncOcrMessage ragDocSyncOcrMessage, Map<Integer, String> ocrData) throws Exception {
        
        List<DocumentUnitEntity> savedUnits = new ArrayList<>();
        
        // 保存原文到DocumentUnitEntity
        for (int pageIndex = 0; pageIndex < ocrData.size(); pageIndex++) {
            String content = ocrData.get(pageIndex);
            
            DocumentUnitEntity documentUnitEntity = new DocumentUnitEntity();
            documentUnitEntity.setContent(content);  // 纯原文内容
            documentUnitEntity.setPage(pageIndex);
            documentUnitEntity.setFileId(ragDocSyncOcrMessage.getFileId());
            documentUnitEntity.setIsVector(false);  // 标记未向量化
            documentUnitEntity.setIsOcr(true);
            
            documentUnitRepository.checkInsert(documentUnitEntity);
            savedUnits.add(documentUnitEntity);
        }
        
        // 第二阶段：异步处理向量化
        CompletableFuture.runAsync(() -> {
            try {
                vectorProcessor.processDocumentUnits(savedUnits);
                log.info("Vector processing completed for file: {}", ragDocSyncOcrMessage.getFileId());
            } catch (Exception e) {
                log.error("Vector processing failed for file: {}", ragDocSyncOcrMessage.getFileId(), e);
            }
        });
    }
    
    private List<ProcessedSegment> processRawSegments(String markdown, RagDocSyncOcrMessage message) {
        // 构建处理上下文
        ProcessingContext context = ProcessingContext.from(message, userModelConfigResolver);
        
        // 使用纯原文模式处理
        StructuralMarkdownProcessor processor = new StructuralMarkdownProcessor();
        processor.setRawMode(true);  // 启用原文模式
        
        return processor.processToSegments(markdown, context);
    }
}
```

### 4.3 检索逻辑改进

#### 基于现有EmbeddingDomainService的搜索增强
```java
// 现有的EmbeddingDomainService已经支持搜索和回溯
// 在ragDoc方法中：
// 1. 向量搜索得到DocumentUnitEntity
// 2. 设置相似度分数
// 3. 直接返回原文内容

public List<DocumentUnitEntity> enhancedRagDoc(List<String> dataSetId, String question, 
        Integer maxResults, Double minScore) {
    // 使用现有的ragDoc方法
    return embeddingDomainService.ragDoc(dataSetId, question, maxResults, minScore, 
            true, 2, embeddingConfig, true);
}

// 如果需要同时返回向量文本和原文，可以扩展DocumentUnitEntity或创建新的DTO
public class RagSearchResult {
    private String vectorText;      // 从vector_store.text获取
    private String originalText;    // 从DocumentUnitEntity.content获取
    private Double similarityScore;
    private Map<String, Object> metadata;
}
```

## 5. 数据库设计

### 5.1 现有表结构复用

#### DocumentUnitEntity（原文存储表）
```sql
-- 保持现有结构，用于存储纯原文
CREATE TABLE document_unit (
    id VARCHAR(36) PRIMARY KEY,
    file_id VARCHAR(36) NOT NULL,
    page INT NOT NULL,
    content TEXT NOT NULL,           -- 存储纯原文markdown
    is_vector BOOLEAN DEFAULT FALSE, -- 是否已处理向量化
    is_ocr BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### vector_store（向量存储表）
```sql
-- 使用现有的向量存储表
CREATE TABLE public.vector_store (
    embedding_id uuid primary key not null,
    embedding vector(1024),
    text text,
    metadata json
);

-- 现有索引
CREATE INDEX idx_vector_store_text_fts ON vector_store USING gin ((to_tsvector('zh_cn'::regconfig, text)));
CREATE INDEX idx_vector_store_metadata_dataset ON vector_store USING btree (((metadata ->> 'datasetId'::text)));
CREATE INDEX idx_vector_store_metadata_file ON vector_store USING btree (((metadata ->> 'fileId'::text)));
CREATE INDEX idx_vector_store_dataset_text ON vector_store USING btree (((metadata ->> 'datasetId'::text))) WHERE (text IS NOT NULL);
```

**现有metadata JSON结构**：
```json
{
    "DATA_SET_ID": "a8d09a8821a3157fc74e3e7ddf74fc68",
    "DOCUMENT_ID": "8d8e22a658c125c54acbf37c9d8b8fd6",  // 关联DocumentUnitEntity
    "FILE_ID": "5bb1568a609f858862bcf58e1d0e1c3a",
    "FILE_NAME": "计费.md"
}
```

### 5.2 索引优化
```sql
-- 针对新增查询优化
CREATE INDEX idx_vector_store_metadata_document ON vector_store USING btree (((metadata ->> 'DOCUMENT_ID'::text)));
CREATE INDEX idx_document_unit_vector_status ON document_unit (file_id, is_vector);
```

## 6. 配置参数

### 6.1 简化配置
```yaml
rag:
  vector:
    max-length: 1800                  # 向量片段最大长度
    min-length: 200                   # 向量片段最小长度
    overlap-size: 100                 # 重叠区域大小
```

## 7. 性能考虑

### 7.1 性能优化策略

#### 批量处理
```java
@Service
public class BatchVectorProcessor {
    
    @Value("${rag.vector.batch-size:50}")
    private int batchSize;
    
    public void processBatch(List<DocumentUnitEntity> units) {
        List<List<DocumentUnitEntity>> batches = Lists.partition(units, batchSize);
        
        for (List<DocumentUnitEntity> batch : batches) {
            processBatchInternal(batch);
        }
    }
    
    private void processBatchInternal(List<DocumentUnitEntity> batch) {
        List<DocumentChunkEntity> allChunks = new ArrayList<>();
        
        // 批量翻译
        for (DocumentUnitEntity unit : batch) {
            String translated = translator.translateSpecialNodes(unit.getContent());
            List<String> vectorTexts = splitter.splitIfNeeded(translated);
            allChunks.addAll(createChunks(vectorTexts, unit));
        }
        
        // 批量发送向量化消息到MQ
        for (DocumentUnitEntity unit : batch) {
            applicationContext.publishEvent(
                new RagDocSyncStorageEvent<>(unit, EventType.DOC_SYNC_RAG)
            );
        }
    }
}
```

#### 异步处理
```java
@Component
public class AsyncVectorProcessor {
    
    @Async("vectorProcessingExecutor")
    public CompletableFuture<Void> processAsync(List<DocumentUnitEntity> units) {
        try {
            vectorProcessor.processDocumentUnits(units);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}

@Configuration
public class AsyncConfig {
    
    @Bean("vectorProcessingExecutor")
    public TaskExecutor vectorProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("vector-processing-");
        executor.initialize();
        return executor;
    }
}
```

### 7.2 内存优化
```java
@Component
public class MemoryOptimizedProcessor {
    
    // 使用流式处理，避免大文档占用过多内存
    public void processLargeFile(String fileId) {
        try (Stream<DocumentUnitEntity> stream = documentUnitRepository.streamByFileId(fileId)) {
            stream.forEach(this::processSingleUnit);
        }
    }
    
    // 及时释放翻译结果内存
    private void processSingleUnit(DocumentUnitEntity unit) {
        String translated = null;
        try {
            translated = translator.translateSpecialNodes(unit.getContent());
            List<String> chunks = splitter.splitIfNeeded(translated);
            saveChunks(chunks, unit);
        } finally {
            translated = null;  // 显式释放
            System.gc();        // 建议垃圾回收（可选）
        }
    }
}
```

## 8. 测试方案

### 8.1 单元测试

#### VectorSegmentProcessor测试
```java
@SpringBootTest
class VectorSegmentProcessorTest {
    
    @Autowired
    private VectorSegmentProcessor processor;
    
    @Test
    void testProcessSingleUnit() {
        // 准备测试数据
        DocumentUnitEntity unit = createTestUnit();
        
        // 执行处理
        processor.processDocumentUnits(List.of(unit));
        
        // 验证结果
        List<DocumentChunkEntity> chunks = chunkRepository.findBySourceUnitId(unit.getId());
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getMetadata()).contains("source_document_unit_id");
    }
    
    @Test
    void testSecondarySegmentSplitter() {
        String longContent = generateLongContent(3000);  // 超过1800字符
        
        List<String> chunks = splitter.splitIfNeeded(longContent);
        
        assertThat(chunks.size()).isGreaterThan(1);
        chunks.forEach(chunk -> {
            assertThat(chunk.length()).isLessThanOrEqualTo(1800);
        });
    }
}
```

#### 性能测试
```java
@Test
void testBatchProcessingPerformance() {
    // 创建1000个测试文档单元
    List<DocumentUnitEntity> units = createTestUnits(1000);
    
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    
    processor.processBatch(units);
    
    stopWatch.stop();
    
    // 验证性能指标
    assertThat(stopWatch.getTotalTimeMillis()).isLessThan(60000);  // 1分钟内完成
}
```

### 8.2 集成测试

#### 端到端流程测试
```java
@SpringBootTest
@Transactional
class RagE2ETest {
    
    @Test
    void testCompleteRagFlow() {
        // 1. 上传markdown文档
        String markdown = loadTestMarkdown();
        RagDocSyncOcrMessage message = createTestMessage();
        
        // 2. 执行文档处理
        markdownStrategy.handle(message, "MARKDOWN");
        
        // 3. 等待异步处理完成
        await().atMost(30, SECONDS).until(() -> 
            documentUnitRepository.countByFileIdAndIsVector(message.getFileId(), true) > 0
        );
        
        // 4. 验证原文存储
        List<DocumentUnitEntity> units = documentUnitRepository.findByFileId(message.getFileId());
        assertThat(units).isNotEmpty();
        assertThat(units.get(0).getContent()).contains("```java");  // 保持原始格式
        
        // 5. 验证向量存储
        List<DocumentChunkEntity> chunks = chunkRepository.findByFileId(message.getFileId());
        assertThat(chunks.size()).isGreaterThanOrEqualTo(units.size());  // 可能一对多
        
        // 6. 验证搜索功能
        List<RagSearchResult> results = ragSearchService.search("Java代码", message.getFileId());
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getOriginalText()).contains("```java");  // 返回原文
        assertThat(results.get(0).getVectorText()).doesNotContain("```");  // 向量文本已翻译
    }
}
```

## 9. 风险评估与缓解

### 9.1 主要风险

#### 风险1：存储空间增加
- **问题**：双通道存储导致存储量翻倍
- **影响**：成本增加，查询性能可能下降
- **缓解措施**：
  - 实现数据压缩
  - 提供清理策略
  - 监控存储使用量

#### 风险2：处理时间延长
- **问题**：二次分割和翻译增加处理时间
- **影响**：用户体验下降
- **缓解措施**：
  - 异步处理
  - 批量优化
  - 进度反馈

#### 风险3：系统复杂度提升
- **问题**：新增组件和流程增加维护成本
- **影响**：开发和运维难度增加
- **缓解措施**：
  - 完善文档
  - 充分测试
  - 监控告警

### 9.2 降级策略

#### 翻译服务降级
```java
@Component
public class FallbackTranslator implements SpecialNodeTranslator {
    
    @Override
    public String translateSpecialNodes(String content) {
        // 降级：不翻译，直接返回原文
        return content;
    }
}
```

#### 分割服务降级
```java
@Component
public class SimpleSplitter implements SecondarySegmentSplitter {
    
    @Override
    public List<String> splitIfNeeded(String content) {
        // 降级：简单截断分割
        if (content.length() <= maxLength) {
            return List.of(content);
        }
        
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < content.length(); i += maxLength) {
            chunks.add(content.substring(i, Math.min(i + maxLength, content.length())));
        }
        return chunks;
    }
}
```


## 10. 总结

### 10.1 预期效果

通过实施本架构设计，预期达到以下效果：

1. **彻底解决长度预测问题**：先翻译后分割，基于真实长度进行分段
2. **提升翻译质量**：无长度限制的翻译，保证内容质量  
3. **优化向量化效果**：基于高质量译文进行向量化，提升搜索准确性
4. **保持原文完整性**：原文通过DocumentUnitEntity永久保存
5. **复用现有架构**：基于现有的EmbeddingDomainService和向量化流程

### 10.2 实施建议

1. **分阶段实施**：
   - 第一阶段：实现VectorSegmentProcessor和翻译逻辑
   - 第二阶段：集成到现有的MarkdownRagDocSyncOcrStrategyImpl
   - 第三阶段：优化和性能调优

2. **复用现有组件**：
   - 使用现有的EmbeddingDomainService进行向量化
   - 使用现有的RagDocStorageConsumer处理消息队列
   - 使用现有的vector_store表和metadata格式

3. **关键注意事项**：
   - 保持标题上下文在二次分割中
   - 通过DOCUMENT_ID字段关联原文和向量
   - 利用现有的MQ异步处理机制

### 10.3 与现有系统的集成

本方案完全基于现有架构：
- **存储层**：复用DocumentUnitEntity和vector_store表
- **处理层**：复用EmbeddingDomainService和消息队列机制  
- **检索层**：复用现有的ragDoc方法和搜索逻辑
- **配置层**：简化配置，仅保留必要参数