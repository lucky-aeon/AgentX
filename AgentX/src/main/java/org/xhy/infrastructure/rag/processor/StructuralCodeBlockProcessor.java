package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.util.ast.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.straegy.StructuralTokenProcessor;

import java.util.HashMap;
import java.util.Map;

/** 结构化代码块处理器 职责：仅识别代码块结构，提取语言和原始内容，不进行大模型处理
 * 
 * @author claude */
@Component
public class StructuralCodeBlockProcessor implements StructuralTokenProcessor {

    private static final Logger log = LoggerFactory.getLogger(StructuralCodeBlockProcessor.class);

    @Override
    public boolean canProcess(Node node) {
        return node instanceof FencedCodeBlock || node instanceof IndentedCodeBlock;
    }

    @Override
    public ProcessedSegment parseStructure(Node node) {
        try {
            String codeContent;
            String language = null;

            if (node instanceof FencedCodeBlock) {
                FencedCodeBlock codeBlock = (FencedCodeBlock) node;
                codeContent = codeBlock.getContentChars().toString().trim();
                // 获取语言标识
                if (codeBlock.getInfo() != null && !codeBlock.getInfo().isBlank()) {
                    language = codeBlock.getInfo().toString().trim();
                }
            } else if (node instanceof IndentedCodeBlock) {
                IndentedCodeBlock codeBlock = (IndentedCodeBlock) node;
                codeContent = codeBlock.getContentChars().toString().trim();
                language = "text"; // 缩进代码块默认为文本
            } else {
                codeContent = node.getChars().toString().trim();
                language = "unknown";
            }

            // 构建结构化元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "code");
            metadata.put("language", language != null ? language : "unknown");
            metadata.put("raw_content", codeContent);
            metadata.put("lines", codeContent.split("\n").length);
            metadata.put("is_fenced", node instanceof FencedCodeBlock);

            // ✅ 关键：保留原始内容，不进行智能处理
            String displayContent = String.format("```%s\n%s\n```", language != null ? language : "", codeContent);

            ProcessedSegment segment = new ProcessedSegment(displayContent, "code", metadata);

            log.debug("Parsed code block structure: language={}, lines={}, type={}", language, metadata.get("lines"),
                    node.getClass().getSimpleName());

            return segment;

        } catch (Exception e) {
            log.error("Failed to parse code block structure", e);
            // 回退方案：返回原始文本
            String rawText = node.getChars().toString();
            Map<String, Object> fallbackMetadata = new HashMap<>();
            fallbackMetadata.put("type", "code");
            fallbackMetadata.put("language", "unknown");
            fallbackMetadata.put("raw_content", rawText);
            fallbackMetadata.put("parse_error", e.getMessage());

            return new ProcessedSegment(rawText, "code", fallbackMetadata);
        }
    }

    @Override
    public int getPriority() {
        return 5; // 高优先级，与原CodeBlockTokenProcessor保持一致
    }

    @Override
    public String getType() {
        return "code";
    }
}