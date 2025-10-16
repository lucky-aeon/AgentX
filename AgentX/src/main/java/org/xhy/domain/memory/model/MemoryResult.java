package org.xhy.domain.memory.model;

import java.util.List;

/** 记忆检索结果（用于组装上下文） */
public class MemoryResult {
    private String itemId;
    private MemoryType type;
    private String text;
    private Float importance;
    private Double score;
    private List<String> tags;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

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

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}

