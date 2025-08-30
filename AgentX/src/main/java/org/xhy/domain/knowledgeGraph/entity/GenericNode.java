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
 * 使用Spring Data Neo4j进行图数据库映射
 * 
 * @author zang
 * @since 1.0.0
 */
@Node("GenericNode")
public class GenericNode {

    /**
     * 节点的全局唯一标识符，作为业务主键
     * 用于在图数据库中唯一标识一个节点
     */
    @Id
    private String id;

    /**
     * 动态标签集合，用于表示节点的类型
     * 例如：{"人物", "技术专家"} 表示这是一个人物节点且是技术专家
     * 标签用于图查询中的节点分类和过滤
     */
    @DynamicLabels
    private Set<String> labels;

    /**
     * 动态属性映射，存储节点的所有自定义属性
     * 支持任意数量和类型的属性，在运行时动态确定
     * 属性值可以是基本类型、字符串、集合等Neo4j支持的数据类型
     */
    @CompositeProperty
    private Map<String, Object> properties;

    /**
     * 创建时间戳
     * 记录节点在图数据库中的创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间戳
     * 记录节点最后一次更新的时间
     */
    private LocalDateTime updatedAt;

    /**
     * 默认构造函数
     * 初始化标签集合、属性映射和时间戳
     */
    public GenericNode() {
        this.labels = new HashSet<>();
        this.properties = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 带ID的构造函数
     * 
     * @param id 节点唯一标识符
     */
    public GenericNode(String id) {
        this();
        this.id = id;
    }

    /**
     * 添加标签到节点
     * 标签用于节点分类和查询过滤
     * 
     * @param label 要添加的标签，不能为null或空字符串
     */
    public void addLabel(String label) {
        if (this.labels == null) {
            this.labels = new HashSet<>();
        }
        this.labels.add(label);
    }

    /**
     * 从节点移除指定标签
     * 
     * @param label 要移除的标签
     */
    public void removeLabel(String label) {
        if (this.labels != null) {
            this.labels.remove(label);
        }
    }

    /**
     * 检查节点是否包含指定标签
     * 
     * @param label 要检查的标签
     * @return true如果包含该标签，false如果不包含
     */
    public boolean hasLabel(String label) {
        return this.labels != null && this.labels.contains(label);
    }

    /**
     * 设置节点属性
     * 设置属性值并更新时间戳
     * 
     * @param key 属性名，不能为null
     * @param value 属性值，可以为null
     */
    public void setProperty(String key, Object value) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        this.properties.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取节点属性值
     * 
     * @param key 属性名
     * @return 属性值，如果不存在则返回null
     */
    public Object getProperty(String key) {
        return this.properties != null ? this.properties.get(key) : null;
    }

    /**
     * 获取字符串类型的属性值
     * 自动将属性值转换为字符串
     * 
     * @param key 属性名
     * @return 属性的字符串表示，如果不存在则返回null
     */
    public String getStringProperty(String key) {
        Object value = getProperty(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 移除指定属性
     * 移除属性并更新时间戳
     * 
     * @param key 要移除的属性名
     */
    public void removeProperty(String key) {
        if (this.properties != null) {
            this.properties.remove(key);
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 检查是否包含指定属性
     * 
     * @param key 属性名
     * @return true如果包含该属性，false如果不包含
     */
    public boolean hasProperty(String key) {
        return this.properties != null && this.properties.containsKey(key);
    }

    /**
     * 获取节点的名称
     * 从name属性获取节点名称
     * 
     * @return 节点名称，如果没有设置则返回null
     */
    public String getName() {
        return getStringProperty("name");
    }

    /**
     * 设置节点的名称
     * 将名称存储在name属性中
     * 
     * @param name 节点名称
     */
    public void setName(String name) {
        setProperty("name", name);
    }

    /**
     * 获取节点的描述
     * 从description属性获取节点描述
     * 
     * @return 节点描述，如果没有设置则返回null
     */
    public String getDescription() {
        return getStringProperty("description");
    }

    /**
     * 设置节点的描述
     * 将描述存储在description属性中
     * 
     * @param description 节点描述
     */
    public void setDescription(String description) {
        setProperty("description", description);
    }

    // Standard getters and setters

    /**
     * 获取节点ID
     * 
     * @return 节点的唯一标识符
     */
    public String getId() {
        return id;
    }

    /**
     * 设置节点ID
     * 
     * @param id 节点的唯一标识符
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取节点标签集合
     * 
     * @return 标签集合的只读视图
     */
    public Collection<String> getLabels() {
        return labels;
    }

    /**
     * 设置节点标签集合
     * 
     * @param labels 新的标签集合
     */
    public void setLabels(Set<String> labels) {
        this.labels = labels;
    }

    /**
     * 获取节点属性映射
     * 
     * @return 属性映射
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * 设置节点属性映射
     * 设置属性映射并更新时间戳
     * 
     * @param properties 新的属性映射
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取创建时间
     * 
     * @return 节点创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间
     * 
     * @param createdAt 节点创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间
     * 
     * @return 节点最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间
     * 
     * @param updatedAt 节点更新时间
     */
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