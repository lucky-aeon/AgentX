package org.xhy.infrastructure.rag.processor;

import com.vladsch.flexmark.util.ast.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.straegy.StructuralTokenProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 结构化公式处理器 职责：仅识别数学公式结构，提取LaTeX代码和公式类型，不进行大模型翻译
 * 
 * @author claude */
@Component
public class StructuralFormulaProcessor implements StructuralTokenProcessor {

    private static final Logger log = LoggerFactory.getLogger(StructuralFormulaProcessor.class);

    // 匹配LaTeX数学公式的正则表达式
    private static final Pattern INLINE_MATH_PATTERN = Pattern.compile("\\$([^$]+)\\$");
    private static final Pattern BLOCK_MATH_PATTERN = Pattern.compile("\\$\\$([^$]+)\\$\\$");

    @Override
    public boolean canProcess(Node node) {
        // 检查节点文本是否包含数学公式
        if (node != null) {
            String text = node.getChars().toString();
            return containsMathFormula(text);
        }
        return false;
    }

    @Override
    public ProcessedSegment parseStructure(Node node) {
        try {
            String text = node.getChars().toString();

            // 提取公式信息
            List<FormulaInfo> formulas = extractFormulas(text);

            // 构建结构化元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "formula");
            metadata.put("raw_content", text);
            metadata.put("formula_count", formulas.size());
            metadata.put("formulas", formulas);

            // 统计公式类型
            long inlineCount = formulas.stream().filter(f -> "inline".equals(f.type)).count();
            long blockCount = formulas.stream().filter(f -> "block".equals(f.type)).count();
            metadata.put("inline_count", inlineCount);
            metadata.put("block_count", blockCount);

            // ✅ 关键：保留原始内容和结构化信息，不进行智能翻译
            String displayContent = String.format("数学公式内容（%d个公式：%d个行内，%d个块级）：\n%s", formulas.size(), inlineCount,
                    blockCount, text);

            ProcessedSegment segment = new ProcessedSegment(displayContent, "formula", metadata);

            log.debug("Parsed formula structure: {} formulas ({} inline, {} block)", formulas.size(), inlineCount,
                    blockCount);

            return segment;

        } catch (Exception e) {
            log.error("Failed to parse formula structure", e);
            // 回退方案：返回原始文本
            String rawText = node.getChars().toString();
            Map<String, Object> fallbackMetadata = new HashMap<>();
            fallbackMetadata.put("type", "formula");
            fallbackMetadata.put("raw_content", rawText);
            fallbackMetadata.put("parse_error", e.getMessage());

            return new ProcessedSegment("公式内容：" + rawText, "formula", fallbackMetadata);
        }
    }

    @Override
    public int getPriority() {
        return 10; // 高优先级，与原FormulaTokenProcessor保持一致
    }

    @Override
    public String getType() {
        return "formula";
    }

    /** 检查文本是否包含数学公式 */
    private boolean containsMathFormula(String text) {
        return INLINE_MATH_PATTERN.matcher(text).find() || BLOCK_MATH_PATTERN.matcher(text).find();
    }

    /** 提取文本中的所有公式信息 */
    private List<FormulaInfo> extractFormulas(String text) {
        List<FormulaInfo> formulas = new ArrayList<>();

        // 提取块级公式 $$...$$
        Matcher blockMatcher = BLOCK_MATH_PATTERN.matcher(text);
        while (blockMatcher.find()) {
            String formula = blockMatcher.group(1).trim();
            String fullMatch = blockMatcher.group(0);
            formulas.add(new FormulaInfo("block", formula, fullMatch, blockMatcher.start(), blockMatcher.end()));
        }

        // 提取行内公式 $...$
        Matcher inlineMatcher = INLINE_MATH_PATTERN.matcher(text);
        while (inlineMatcher.find()) {
            String formula = inlineMatcher.group(1).trim();
            String fullMatch = inlineMatcher.group(0);
            // 确保不是块级公式的一部分
            if (!isPartOfBlockFormula(inlineMatcher.start(), inlineMatcher.end(), formulas)) {
                formulas.add(new FormulaInfo("inline", formula, fullMatch, inlineMatcher.start(), inlineMatcher.end()));
            }
        }

        return formulas;
    }

    /** 检查行内公式是否是块级公式的一部分 */
    private boolean isPartOfBlockFormula(int start, int end, List<FormulaInfo> blockFormulas) {
        for (FormulaInfo blockFormula : blockFormulas) {
            if ("block".equals(blockFormula.type) && start >= blockFormula.start && end <= blockFormula.end) {
                return true;
            }
        }
        return false;
    }

    /** 公式信息内部类 */
    public static class FormulaInfo {
        public final String type; // "inline" 或 "block"
        public final String latex; // LaTeX代码
        public final String fullMatch; // 完整匹配内容
        public final int start; // 起始位置
        public final int end; // 结束位置

        public FormulaInfo(String type, String latex, String fullMatch, int start, int end) {
            this.type = type;
            this.latex = latex;
            this.fullMatch = fullMatch;
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return String.format("FormulaInfo{type='%s', latex='%s'}", type, latex);
        }
    }
}