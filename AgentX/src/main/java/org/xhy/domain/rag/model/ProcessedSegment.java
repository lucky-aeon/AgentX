package org.xhy.domain.rag.model;

import org.xhy.domain.rag.model.enums.SegmentType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** 处理后的Markdown段落
 * 
 * 支持占位符机制，可以包含特殊节点的语义段落
 * 
 * @author claude */
public class ProcessedSegment {

    /** 处理后的可搜索文本内容（可能包含占位符） */
    private String content;

    /** 段落类型 */
    private SegmentType type;

    /** 元数据信息 */
    private Map<String, Object> metadata;

    /** 特殊节点集合（占位符 -> 特殊节点） */
    private Map<String, SpecialNode> specialNodes;

    /** 在文档中的顺序 */
    private int order;

    public ProcessedSegment() {
        this.metadata = new HashMap<>();
        this.specialNodes = new LinkedHashMap<>();
    }

    public ProcessedSegment(String content, SegmentType type, Map<String, Object> metadata) {
        this.content = content;
        this.type = type;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.specialNodes = new LinkedHashMap<>();
    }
    
    // 兼容旧版本的构造函数（字符串类型）
    @Deprecated
    public ProcessedSegment(String content, String type, Map<String, Object> metadata) {
        this(content, SegmentType.fromValue(type), metadata);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public SegmentType getType() {
        return type;
    }

    public void setType(SegmentType type) {
        this.type = type;
    }
    
    // 兼容旧版本的方法（字符串类型）
    @Deprecated
    public void setType(String type) {
        this.type = SegmentType.fromValue(type);
    }
    
    // 兼容旧版本，返回字符串值
    @Deprecated
    public String getTypeValue() {
        return type != null ? type.getValue() : null;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    public Map<String, SpecialNode> getSpecialNodes() {
        return specialNodes;
    }

    public void setSpecialNodes(Map<String, SpecialNode> specialNodes) {
        this.specialNodes = specialNodes;
    }

    /** 添加特殊节点 */
    public void addSpecialNode(SpecialNode specialNode) {
        if (this.specialNodes == null) {
            this.specialNodes = new LinkedHashMap<>();
        }
        this.specialNodes.put(specialNode.getPlaceholder(), specialNode);
    }

    /** 检查是否包含特殊节点 */
    public boolean hasSpecialNodes() {
        return specialNodes != null && !specialNodes.isEmpty();
    }

    /** 获取指定类型的特殊节点数量 */
    public long getSpecialNodeCount(SegmentType nodeType) {
        if (specialNodes == null) {
            return 0;
        }
        return specialNodes.values().stream()
                .filter(node -> node.getNodeType() == nodeType)
                .count();
    }

    /** 应用增强处理，将所有占位符替换为最终内容 */
    public ProcessedSegment applyEnhancements() {
        if (!hasSpecialNodes()) {
            return this;
        }

        String finalContent = content;
        for (SpecialNode node : specialNodes.values()) {
            finalContent = finalContent.replace(node.getPlaceholder(), node.getFinalContent());
        }

        ProcessedSegment result = new ProcessedSegment(finalContent, type, metadata);
        result.setOrder(order);
        // 清空特殊节点，因为已经合并到内容中了
        result.setSpecialNodes(new LinkedHashMap<>());
        return result;
    }

    /** 获取合并后的内容（但不修改当前对象） */
    public String getFinalContent() {
        if (!hasSpecialNodes()) {
            return content;
        }

        String finalContent = content;
        for (SpecialNode node : specialNodes.values()) {
            finalContent = finalContent.replace(node.getPlaceholder(), node.getFinalContent());
        }
        return finalContent;
    }

    @Override
    public String toString() {
        return "ProcessedSegment{" + 
               "content='" + content + '\'' + 
               ", type=" + type + 
               ", order=" + order +
               ", specialNodes=" + (specialNodes != null ? specialNodes.size() : 0) +
               ", metadata=" + metadata + 
               '}';
    }
}