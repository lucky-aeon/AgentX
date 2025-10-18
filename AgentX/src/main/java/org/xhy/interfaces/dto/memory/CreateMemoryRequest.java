package org.xhy.interfaces.dto.memory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/** 手动创建记忆请求 */
public class CreateMemoryRequest {

    @NotBlank
    private String type; // PROFILE/TASK/FACT/EPISODIC

    @NotBlank
    @Size(max = 5000)
    private String text;

    private Float importance; // 0~1

    private List<String> tags;

    private Map<String, Object> data;

    public String getType() {
        return type;
    }
    public void setType(String type) {
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
