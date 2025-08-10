package org.xhy.domain.rag.model;

import org.xhy.domain.rag.model.enums.SegmentType;

import java.util.HashMap;
import java.util.Map;

/** 特殊节点信息
 *
 * 用于支持占位符机制，在语义分段中收集和处理特殊内容节点
 * 
 * @author claude */
public class SpecialNode {

    /** 节点类型 */
    private SegmentType nodeType;

    /** 占位符标识符 */
    private String placeholder;

    /** 原始内容 */
    private String originalContent;

    /** 增强处理后的内容 */
    private String enhancedContent;

    /** 节点特定的元数据 */
    private Map<String, Object> nodeMetadata;

    /** 是否已经被处理过 */
    private boolean processed;

    public SpecialNode() {
        this.nodeMetadata = new HashMap<>();
        this.processed = false;
    }

    public SpecialNode(SegmentType nodeType, String placeholder, String originalContent) {
        this();
        this.nodeType = nodeType;
        this.placeholder = placeholder;
        this.originalContent = originalContent;
    }

    /** 生成占位符
     * 
     * @param nodeType 节点类型
     * @param index 序号
     * @return 占位符字符串 */
    public static String generatePlaceholder(SegmentType nodeType, int index) {
        String typePrefix = nodeType.name().toUpperCase();
        return String.format("{{SPECIAL_NODE_%s_%03d}}", typePrefix, index);
    }

    /** 检查字符串是否为特殊节点占位符 */
    public static boolean isPlaceholder(String text) {
        return text != null && text.matches("\\{\\{SPECIAL_NODE_[A-Z]+_\\d{3}\\}\\}");
    }

    /** 从占位符中提取节点类型 */
    public static SegmentType extractNodeTypeFromPlaceholder(String placeholder) {
        if (!isPlaceholder(placeholder)) {
            throw new IllegalArgumentException("Invalid placeholder format: " + placeholder);
        }

        // 提取类型：{{SPECIAL_NODE_IMAGE_001}} -> IMAGE
        String typeStr = placeholder.replaceAll("\\{\\{SPECIAL_NODE_([A-Z]+)_\\d{3}\\}\\}", "$1");
        return SegmentType.valueOf(typeStr);
    }

    /** 获取最终内容（优先返回增强内容，否则返回原始内容） */
    public String getFinalContent() {
        return enhancedContent != null && !enhancedContent.trim().isEmpty() ? enhancedContent : originalContent;
    }

    /** 标记为已处理 */
    public void markAsProcessed() {
        this.processed = true;
    }

    /** 添加元数据 */
    public void addMetadata(String key, Object value) {
        if (this.nodeMetadata == null) {
            this.nodeMetadata = new HashMap<>();
        }
        this.nodeMetadata.put(key, value);
    }

    // Getters and Setters
    public SegmentType getNodeType() {
        return nodeType;
    }

    public void setNodeType(SegmentType nodeType) {
        this.nodeType = nodeType;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getEnhancedContent() {
        return enhancedContent;
    }

    public void setEnhancedContent(String enhancedContent) {
        this.enhancedContent = enhancedContent;
    }

    public Map<String, Object> getNodeMetadata() {
        return nodeMetadata;
    }

    public void setNodeMetadata(Map<String, Object> nodeMetadata) {
        this.nodeMetadata = nodeMetadata;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    @Override
    public String toString() {
        return "SpecialNode{" + "nodeType=" + nodeType + ", placeholder='" + placeholder + '\'' + ", originalLength="
                + (originalContent != null ? originalContent.length() : 0) + ", enhanced=" + (enhancedContent != null)
                + ", processed=" + processed + '}';
    }
}