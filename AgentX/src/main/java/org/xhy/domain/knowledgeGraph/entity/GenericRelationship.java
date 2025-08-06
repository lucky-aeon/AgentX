package org.xhy.domain.knowledgeGraph.entity;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用动态关系实体类
 * 支持任意类型的关系和任意数量的属性
 * 
 * @author zang
 */
@RelationshipProperties
public class GenericRelationship {

    /**
     * 关系的唯一标识符，由Neo4j自动生成
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * 关系的类型，例如："掌握"、"工作于"、"属于" 等
     */
    private String type;

    /**
     * 动态属性映射，存储关系的所有自定义属性
     * 支持任意数量和类型的属性，在运行时动态确定
     */
    @CompositeProperty
    private Map<String, Object> properties;

    /**
     * 目标节点，关系指向的节点
     */
    @TargetNode
    private GenericNode targetNode;

    /**
     * 创建时间戳
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间戳
     */
    private LocalDateTime updatedAt;

    public GenericRelationship() {
        this.properties = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public GenericRelationship(String type) {
        this();
        this.type = type;
    }

    public GenericRelationship(String type, GenericNode targetNode) {
        this(type);
        this.targetNode = targetNode;
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
     * 获取关系的描述（通常来自description属性）
     */
    public String getDescription() {
        return getStringProperty("description");
    }

    /**
     * 设置关系的描述
     */
    public void setDescription(String description) {
        setProperty("description", description);
    }

    // Standard getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
        this.updatedAt = LocalDateTime.now();
    }

    public GenericNode getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(GenericNode targetNode) {
        this.targetNode = targetNode;
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
        return "GenericRelationship{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", properties=" + properties +
                ", targetNodeId=" + (targetNode != null ? targetNode.getId() : null) +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenericRelationship that = (GenericRelationship) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}