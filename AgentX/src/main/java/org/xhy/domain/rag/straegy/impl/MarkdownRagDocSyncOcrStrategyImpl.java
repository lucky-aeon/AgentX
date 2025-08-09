package org.xhy.domain.rag.straegy.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.dromara.x.file.storage.core.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.message.RagDocSyncOcrMessage;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.FileDetailEntity;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.domain.rag.repository.FileDetailRepository;
import org.xhy.domain.rag.straegy.context.ProcessingContext;
import org.xhy.domain.rag.processor.MarkdownProcessor;
import org.xhy.infrastructure.rag.service.UserModelConfigResolver;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Markdown文档处理策略实现 支持表格、公式、图片的增强处理
 * 
 * @author claude */
@Service("ragDocSyncOcr-MARKDOWN")
public class MarkdownRagDocSyncOcrStrategyImpl extends RagDocSyncOcrStrategyImpl {

    private static final Logger log = LoggerFactory.getLogger(MarkdownRagDocSyncOcrStrategyImpl.class);

    private final MarkdownProcessor markdownProcessor;
    private final DocumentUnitRepository documentUnitRepository;
    private final FileDetailRepository fileDetailRepository;
    private final FileStorageService fileStorageService;
    private final UserModelConfigResolver userModelConfigResolver;

    // 用于存储当前处理的文件ID
    private String currentProcessingFileId;

    public MarkdownRagDocSyncOcrStrategyImpl(@Qualifier("ragEnhancedMarkdownProcessor") MarkdownProcessor markdownProcessor, 
            DocumentUnitRepository documentUnitRepository, FileDetailRepository fileDetailRepository, 
            FileStorageService fileStorageService, UserModelConfigResolver userModelConfigResolver) {
        this.markdownProcessor = markdownProcessor;
        this.documentUnitRepository = documentUnitRepository;
        this.fileDetailRepository = fileDetailRepository;
        this.fileStorageService = fileStorageService;
        this.userModelConfigResolver = userModelConfigResolver;
    }

    @Override
    public void handle(RagDocSyncOcrMessage ragDocSyncOcrMessage, String strategy) throws Exception {
        // 设置当前处理的文件ID
        this.currentProcessingFileId = ragDocSyncOcrMessage.getFileId();

        log.info("Starting Markdown document processing for file: {}", currentProcessingFileId);

        // 调用父类处理逻辑
        super.handle(ragDocSyncOcrMessage, strategy);

        log.info("Completed Markdown document processing for file: {}", currentProcessingFileId);
    }

    @Override
    public void pushPageSize(byte[] bytes, RagDocSyncOcrMessage ragDocSyncOcrMessage) {
        try {
            String markdown = new String(bytes, StandardCharsets.UTF_8);

            // 构建处理上下文
            ProcessingContext context = ProcessingContext.from(ragDocSyncOcrMessage, userModelConfigResolver);

            // 使用同步模式处理Markdown，获取段落数量
            List<ProcessedSegment> segments = markdownProcessor.processToSegments(markdown, context);
            int segmentCount = segments.size();

            ragDocSyncOcrMessage.setPageSize(segmentCount);
            log.info("Markdown document split into {} segments", segmentCount);

            // 更新数据库中的总页数
            if (currentProcessingFileId != null) {
                LambdaUpdateWrapper<FileDetailEntity> wrapper = Wrappers.<FileDetailEntity>lambdaUpdate()
                        .eq(FileDetailEntity::getId, currentProcessingFileId)
                        .set(FileDetailEntity::getFilePageSize, segmentCount);
                fileDetailRepository.update(wrapper);

                log.info("Updated total pages for Markdown file {}: {} segments", currentProcessingFileId,
                        segmentCount);
            }

        } catch (Exception e) {
            log.error("Failed to calculate page size for Markdown document", e);
            ragDocSyncOcrMessage.setPageSize(1); // 回退到单页
        }
    }

    @Override
    public byte[] getFileData(RagDocSyncOcrMessage ragDocSyncOcrMessage, String strategy) {
        try {
            // 从数据库中获取文件详情
            FileDetailEntity fileDetailEntity = fileDetailRepository.selectById(ragDocSyncOcrMessage.getFileId());
            if (fileDetailEntity == null) {
                log.error("File does not exist: {}", ragDocSyncOcrMessage.getFileId());
                return new byte[0];
            }

            // 下载文件内容
            log.info("Downloading Markdown document: {}", fileDetailEntity.getFilename());
            return fileStorageService.download(fileDetailEntity.getUrl()).bytes();

        } catch (Exception e) {
            log.error("Failed to download Markdown file: {}", ragDocSyncOcrMessage.getFileId(), e);
            return new byte[0];
        }
    }

    @Override
    public Map<Integer, String> processFile(byte[] fileBytes, int totalPages) {
        return new HashMap<>(); // 使用带消息参数的重载方法
    }

    @Override
    public Map<Integer, String> processFile(byte[] fileBytes, int totalPages,
            RagDocSyncOcrMessage ragDocSyncOcrMessage) {

        log.info("Processing Markdown document with enhanced processor");

        try {
            String markdown = new String(fileBytes, StandardCharsets.UTF_8);

            // 构建处理上下文
            ProcessingContext context = ProcessingContext.from(ragDocSyncOcrMessage, userModelConfigResolver);

            // 🚀 使用同步处理器进行语义感知处理
            List<ProcessedSegment> finalSegments = markdownProcessor.processToSegments(markdown, context);
            log.info("Synchronous processing completed: {} segments generated", finalSegments.size());
            Map<Integer, String> ocrData = new HashMap<>();

            // 将处理后的段落转换为页面格式（每个段落一页）
            for (int i = 0; i < finalSegments.size(); i++) {
                ProcessedSegment segment = finalSegments.get(i);
                String content = segment.getContent();

                // 为复杂类型添加额外信息
                if (!"text".equals(segment.getType()) && segment.getMetadata() != null) {
                    content = enrichContentWithMetadata(content, segment);
                }

                ocrData.put(i, content);

                log.debug("Processed segment {}/{}: type={}, length={}", i + 1, finalSegments.size(), 
                         segment.getType(), content.length());
            }

            // 更新页面大小（可能与预估的不同）
            if (finalSegments.size() != totalPages) {
                ragDocSyncOcrMessage.setPageSize(finalSegments.size());
                log.info("Updated segment count from {} to {}", totalPages, finalSegments.size());
            }

            log.info("Successfully processed Markdown document into {} segments", ocrData.size());
            return ocrData;

        } catch (Exception e) {
            log.error("Failed to process Markdown document", e);

            // 回退方案：将整个文档作为一个页面
            String fallbackContent = new String(fileBytes, StandardCharsets.UTF_8);
            Map<Integer, String> fallbackData = new HashMap<>();
            fallbackData.put(0, "Markdown文档：" + fallbackContent);
            return fallbackData;
        }
    }

    @Override
    public void insertData(RagDocSyncOcrMessage ragDocSyncOcrMessage, Map<Integer, String> ocrData) throws Exception {

        log.info("Saving Markdown document content, split into {} segments", ocrData.size());

        // 遍历每个段落，将内容保存到数据库
        for (int pageIndex = 0; pageIndex < ocrData.size(); pageIndex++) {
            String content = ocrData.get(pageIndex);

            DocumentUnitEntity documentUnitEntity = new DocumentUnitEntity();
            documentUnitEntity.setContent(content);
            documentUnitEntity.setPage(pageIndex);
            documentUnitEntity.setFileId(ragDocSyncOcrMessage.getFileId());
            documentUnitEntity.setIsVector(false);
            documentUnitEntity.setIsOcr(true);

            if (content == null || content.trim().isEmpty()) {
                documentUnitEntity.setIsOcr(false);
                log.warn("Segment {} is empty", pageIndex + 1);
            }

            // 保存到数据库
            documentUnitRepository.checkInsert(documentUnitEntity);
            log.debug("Saved segment {} content", pageIndex + 1);
        }

        log.info("Markdown document content saved successfully");
    }

    /** 为内容添加元数据信息，增强可搜索性 针对不同类型的内容提供专门的增强逻辑 */
    private String enrichContentWithMetadata(String content, ProcessedSegment segment) {
        Map<String, Object> metadata = segment.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return content;
        }

        String type = segment.getType();

        switch (type) {
            case "table" :
                return enrichTableContent(content, metadata);
            case "image" :
                return enrichImageContent(content, metadata);
            case "formula" :
                return enrichFormulaContent(content, metadata);
            case "code" :
                return enrichCodeContent(content, metadata);
            default :
                return content;
        }
    }

    /** 增强表格内容 - 创建表头与数据的关联描述 */
    private String enrichTableContent(String content, Map<String, Object> metadata) {
        StringBuilder enriched = new StringBuilder(content);

        Object columns = metadata.get("columns");
        Object rows = metadata.get("rows");
        Object structure = metadata.get("structure");

        // 添加基本统计信息
        if (columns != null && rows != null) {
            enriched.append(String.format(" [表格规模：%s列×%s行]", columns, rows));
        }

        // 如果有结构化数据，尝试创建更可读的格式
        if (structure != null) {
            try {
                String structureText = structure.toString();
                String readableTable = makeTableReadable(structureText);
                if (!readableTable.isEmpty()) {
                    enriched.append(" ").append(readableTable);
                }
            } catch (Exception e) {
                log.debug("Failed to process table structure: {}", e.getMessage());
            }
        }

        enriched.append(" [此表格适合查询数据关系和统计信息]");

        return enriched.toString();
    }

    /** 将表格结构转换为更可读的格式 */
    private String makeTableReadable(String structure) {
        if (structure == null || structure.trim().isEmpty()) {
            return "";
        }

        try {
            // 解析表格结构，创建关联描述
            String[] lines = structure.split("\n");
            StringBuilder readable = new StringBuilder();

            String[] headers = null;
            boolean foundHeaders = false;

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("表头：") && !foundHeaders) {
                    String headerLine = line.substring(3);
                    headers = headerLine.split("\\s*\\|\\s*");
                    foundHeaders = true;
                } else if (line.contains("|") && foundHeaders && headers != null) {
                    String[] values = line.split("\\s*\\|\\s*");
                    if (values.length >= headers.length) {
                        readable.append("数据记录：");
                        for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                            if (i > 0)
                                readable.append("，");
                            readable.append(headers[i]).append("为").append(values[i]);
                        }
                        readable.append("；");
                    }
                }
            }

            return readable.toString();
        } catch (Exception e) {
            log.debug("Failed to make table readable: {}", e.getMessage());
            return "";
        }
    }

    /** 增强图片内容 - 优化OCR结果的可搜索性 */
    private String enrichImageContent(String content, Map<String, Object> metadata) {
        StringBuilder enriched = new StringBuilder(content);

        Object url = metadata.get("url");
        Object alt = metadata.get("alt");

        // 添加图片基本信息
        if (url != null) {
            enriched.append(String.format(" [图片地址：%s]", url));
        }

        if (alt != null && !alt.toString().trim().isEmpty()) {
            enriched.append(String.format(" [图片说明：%s]", alt));
        }

        // 标记为可视化内容，便于RAG检索时识别
        enriched.append(" [此内容包含图像信息，适合查询视觉相关问题]");

        // 优化OCR内容格式
        String optimizedContent = optimizeOcrContent(content);
        if (!optimizedContent.equals(content)) {
            return optimizedContent + enriched.substring(content.length());
        }

        return enriched.toString();
    }

    /** 优化OCR内容格式 */
    private String optimizeOcrContent(String content) {
        if (content == null)
            return "";

        // 移除多余的空白字符，保持可读性
        String optimized = content.replaceAll("\\s+", " ").trim();

        // 确保句子结构完整
        if (!optimized.endsWith("。") && !optimized.endsWith("！") && !optimized.endsWith("？")) {
            optimized += "。";
        }

        return optimized;
    }

    /** 增强公式内容 - 添加数学领域标签 */
    private String enrichFormulaContent(String content, Map<String, Object> metadata) {
        StringBuilder enriched = new StringBuilder(content);

        Object originalText = metadata.get("original_text");

        // 分析公式类型
        String formulaType = analyzeFormulaType(originalText != null ? originalText.toString() : content);
        if (!formulaType.isEmpty()) {
            enriched.append(String.format(" [数学领域：%s]", formulaType));
        }

        // 标记为数学内容
        enriched.append(" [此内容为数学公式，适合查询计算和数学推理问题]");

        return enriched.toString();
    }

    /** 分析公式类型 */
    private String analyzeFormulaType(String formula) {
        if (formula == null)
            return "";

        String lower = formula.toLowerCase();

        if (lower.contains("\\int") || lower.contains("\\sum") || lower.contains("\\prod")) {
            return "微积分";
        } else if (lower.contains("\\frac") || lower.contains("\\sqrt")) {
            return "代数";
        } else if (lower.contains("\\sin") || lower.contains("\\cos") || lower.contains("\\tan")) {
            return "三角函数";
        } else if (lower.contains("\\log") || lower.contains("\\ln") || lower.contains("\\exp")) {
            return "对数指数";
        } else if (lower.contains("\\matrix") || lower.contains("\\begin{array}")) {
            return "线性代数";
        } else if (lower.contains("\\lim") || lower.contains("\\to")) {
            return "极限";
        }

        return "数学表达式";
    }

    /** 增强代码内容 - 添加编程语言和功能标签 */
    private String enrichCodeContent(String content, Map<String, Object> metadata) {
        StringBuilder enriched = new StringBuilder(content);

        Object language = metadata.get("language");
        Object lines = metadata.get("lines");

        // 添加编程语言信息
        if (language != null && !"unknown".equals(language)) {
            enriched.append(String.format(" [编程语言：%s]", language));
        }

        // 添加代码规模信息
        if (lines != null) {
            enriched.append(String.format(" [代码行数：%s行]", lines));
        }

        // 标记为代码内容
        enriched.append(" [此内容为程序代码，适合查询编程实现和技术方案问题]");

        return enriched.toString();
    }
}