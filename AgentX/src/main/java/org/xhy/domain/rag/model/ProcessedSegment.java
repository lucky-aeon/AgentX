package org.xhy.domain.rag.model;

import java.util.HashMap;
import java.util.Map;

/** 处理后的Markdown段落
 * 
 * @author claude */
public class ProcessedSegment {

    /** 处理后的可搜索文本内容 */
    private String content;

    /** 段落类型：text, table, formula, image */
    private String type;

    /** 元数据信息 */
    private Map<String, Object> metadata;

    /** 在文档中的顺序 */
    private int order;

    public ProcessedSegment() {
        this.metadata = new HashMap<>();
    }

    public ProcessedSegment(String content, String type, Map<String, Object> metadata) {
        this.content = content;
        this.type = type;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
        return "ProcessedSegment{" + "content='" + content + '\'' + ", type='" + type + '\'' + ", order=" + order
                + ", metadata=" + metadata + '}';
    }
}