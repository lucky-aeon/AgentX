package org.xhy.domain.rag.model;

import org.xhy.domain.rag.model.enums.SegmentType;

import java.util.HashMap;
import java.util.Map;

/** 处理后的Markdown段落
 * 
 * 用于表示文档分段处理后的结果
 * 
 * @author claude */
public class ProcessedSegment {

    /** 处理后的可搜索文本内容（可能包含占位符） */
    private String content;

    /** 段落类型 */
    private SegmentType type;

    /** 元数据信息 */
    private Map<String, Object> metadata;

    /** 在文档中的顺序 */
    private int order;

    public ProcessedSegment() {
        this.metadata = new HashMap<>();
    }

    public ProcessedSegment(String content, SegmentType type, Map<String, Object> metadata) {
        this.content = content;
        this.type = type;
        this.metadata = metadata != null ? metadata : new HashMap<>();
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

    @Override
    public String toString() {
        return "ProcessedSegment{" + "content='" + content + '\'' + ", type=" + type + ", order=" + order
                + ", metadata=" + metadata + '}';
    }
}