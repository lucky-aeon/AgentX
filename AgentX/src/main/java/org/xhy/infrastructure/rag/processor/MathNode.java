package org.xhy.infrastructure.rag.processor;

/** 数学公式节点
 * 
 * 用于在占位符架构中表示数学公式内容的轻量级节点
 * 不依赖Flexmark扩展，基于文本模式识别创建
 * 
 * @author claude */
public class MathNode {
    
    /** 公式类型：行内公式或块级公式 */
    public enum FormulaType {
        INLINE,    // 行内公式 $...$
        BLOCK,     // 块级公式 $$...$$
        LATEX_INLINE,  // LaTeX行内 \(...\)
        LATEX_BLOCK    // LaTeX块级 \[...\]
    }
    
    private final String originalText;  // 原始文本（包含定界符）
    private final String formulaContent; // 纯公式内容（不包含定界符）
    private final FormulaType type;
    private final int startIndex;  // 在原文本中的起始位置
    private final int endIndex;    // 在原文本中的结束位置
    
    public MathNode(String originalText, String formulaContent, FormulaType type, int startIndex, int endIndex) {
        this.originalText = originalText;
        this.formulaContent = formulaContent;
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }
    
    /** 获取原始文本（包含定界符） */
    public String getOriginalText() {
        return originalText;
    }
    
    /** 获取纯公式内容（不包含定界符） */
    public String getFormulaContent() {
        return formulaContent;
    }
    
    /** 获取公式类型 */
    public FormulaType getType() {
        return type;
    }
    
    /** 获取起始位置 */
    public int getStartIndex() {
        return startIndex;
    }
    
    /** 获取结束位置 */
    public int getEndIndex() {
        return endIndex;
    }
    
    /** 是否为块级公式 */
    public boolean isBlockFormula() {
        return type == FormulaType.BLOCK || type == FormulaType.LATEX_BLOCK;
    }
    
    /** 是否为行内公式 */
    public boolean isInlineFormula() {
        return type == FormulaType.INLINE || type == FormulaType.LATEX_INLINE;
    }
    
    /** 生成显示用的Markdown格式 */
    public String toMarkdown() {
        return originalText; // 保持原始格式
    }
    
    /** 检查公式内容是否有效 */
    public boolean isValid() {
        return formulaContent != null && !formulaContent.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("MathNode{type=%s, content='%s', range=[%d,%d]}", 
            type, formulaContent, startIndex, endIndex);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        MathNode mathNode = (MathNode) obj;
        return startIndex == mathNode.startIndex && 
               endIndex == mathNode.endIndex &&
               type == mathNode.type &&
               originalText.equals(mathNode.originalText);
    }
    
    @Override
    public int hashCode() {
        int result = originalText.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + startIndex;
        result = 31 * result + endIndex;
        return result;
    }
}