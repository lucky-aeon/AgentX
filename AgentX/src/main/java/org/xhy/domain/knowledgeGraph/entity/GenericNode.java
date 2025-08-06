package org.xhy.domain.knowledgeGraph.entity;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 通用动态节点实体类
 * 支持任意类型的实体和任意数量的属性，无需预先定义具体的实体类
 * 
 * @author zang
 */
@Node("GenericNode")
public class GenericNode {

    /**
     * 节点的全局唯一标识符，作为业务主键
     */
    @Id
    private String id;

    /**
     * 动态标签集合，用于表示节点的类型
     * 例如：{"人物", "技术专家"} 表示这是一个人物节点且是技术专家
     */
    @DynamicLabels
    private Set<String> labels;

    /**
     * 动态属性映射，存储节点的所有自定义属性
     * 支持任意数量和类型的属性，在运行时动态确定
     */
    @CompositeProperty
    private Map<String, Object> properties;

    /**
     * 创建时间戳
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间戳  
     */
    private LocalDateTime updatedAt;

    public GenericNode() {
        this.labels = new HashSet<>();
        this.properties = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public GenericNode(String id) {
        this();
        this.id = id;
    }

    /**
     * 添加标签
     */
    public void addLabel(String label) {
        if (this.labels == null) {
            this.labels = new HashSet<>();
        }
        this.labels.add(label);
    }

    /**
     * 移除标签
     */
    public void removeLabel(String label) {
        if (this.labels != null) {
            this.labels.remove(label);
        }
    }

    /**
     * 检查是否包含指定标签
     */
    public boolean hasLabel(String label) {
        return this.labels != null && this.labels.contains(label);
    }

    /**
     * 设置属性
     */
    public void setProperty(String key, Object value) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        this.properties.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取属性
     */
    public Object getProperty(String key) {
        return this.properties != null ? this.properties.get(key) : null;
    }

    /**
     * 获取字符串类型属性
     */
    public String getStringProperty(String key) {
        Object value = getProperty(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 移除属性
     */
    public void removeProperty(String key) {
        if (this.properties != null) {
            this.properties.remove(key);
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 检查是否包含指定属性
     */
    public boolean hasProperty(String key) {
        return this.properties != null && this.properties.containsKey(key);
    }

    /**
     * 获取节点的名称（通常来自name属性）
     */
    public String getName() {
        return getStringProperty("name");
    }

    /**
     * 设置节点的名称
     */
    public void setName(String name) {
        setProperty("name", name);
    }

    /**
     * 获取节点的描述（通常来自description属性）
     */
    public String getDescription() {
        return getStringProperty("description");
    }

    /**
     * 设置节点的描述
     */
    public void setDescription(String description) {
        setProperty("description", description);
    }

    // Standard getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Collection<String> getLabels() {
        return labels;
    }

    public void setLabels(Set<String> labels) {
        this.labels = labels;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "GenericNode{" +
                "id='" + id + '\'' +
                ", labels=" + labels +
                ", properties=" + properties +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenericNode that = (GenericNode) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}