package org.xhy.domain.memory.model;

import java.util.List;
import java.util.Map;

/** 记忆候选（抽取器输出） */
public class CandidateMemory {
    private MemoryType type;
    private String text;
    private Float importance;
    private List<String> tags;
    private Map<String, Object> data;

    public MemoryType getType() {
        return type;
    }

    public void setType(MemoryType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Float getImportance() {
        return importance;
    }

    public void setImportance(Float importance) {
        this.importance = importance;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
