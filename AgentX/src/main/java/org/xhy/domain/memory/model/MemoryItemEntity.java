package org.xhy.domain.memory.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.infrastructure.converter.ListStringConverter;
import org.xhy.infrastructure.converter.MapConverter;
import org.xhy.infrastructure.entity.BaseEntity;

import java.util.List;
import java.util.Map;

/** 记忆条目实体（memory_items） */
@TableName("memory_items")
public class MemoryItemEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("type")
    private String type; // 使用字符串存储，取值见 MemoryType

    @TableField("text")
    private String text;

    @TableField(value = "data", typeHandler = MapConverter.class)
    private Map<String, Object> data;

    @TableField("importance")
    private Float importance;

    @TableField(value = "tags", typeHandler = ListStringConverter.class)
    private List<String> tags;

    @TableField("source_session_id")
    private String sourceSessionId;

    @TableField("dedupe_hash")
    private String dedupeHash;

    @TableField("status")
    private Integer status; // 1=active, 0=archived/deleted

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

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

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
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

    public String getSourceSessionId() {
        return sourceSessionId;
    }

    public void setSourceSessionId(String sourceSessionId) {
        this.sourceSessionId = sourceSessionId;
    }

    public String getDedupeHash() {
        return dedupeHash;
    }

    public void setDedupeHash(String dedupeHash) {
        this.dedupeHash = dedupeHash;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

