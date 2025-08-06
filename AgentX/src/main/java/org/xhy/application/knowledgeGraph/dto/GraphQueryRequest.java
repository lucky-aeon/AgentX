package org.xhy.application.knowledgeGraph.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 图查询请求DTO
 * 用于定义复杂的动态图查询条件
 * 
 * @author zang
 */
public class GraphQueryRequest {

    /**
     * 查询的起始节点列表
     */
    @JsonProperty("startNodes")
    @Valid
    private List<NodeFilter> startNodes;

    /**
     * 遍历步骤列表，定义图遍历路径
     */
    @JsonProperty("traversals")
    @Valid
    private List<TraversalStep> traversals;

    /**
     * 过滤条件列表
     */
    @JsonProperty("filters")
    @Valid
    private List<QueryFilter> filters;

    /**
     * 返回结果定义
     */
    @JsonProperty("returnDefinition")
    @Valid
    private ReturnDefinition returnDefinition;

    /**
     * 查询限制条件
     */
    @JsonProperty("limit")
    private Integer limit = 100;

    /**
     * 节点过滤器
     */
    public static class NodeFilter {
        @JsonProperty("label")
        private String label;

        @JsonProperty("property")
        private String property;

        @JsonProperty("value")
        private Object value;

        @JsonProperty("operator")
        private String operator = "eq"; // eq, contains, in, gt, lt, etc.

        public NodeFilter() {}

        public NodeFilter(String label, String property, Object value) {
            this.label = label;
            this.property = property;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }
    }

    /**
     * 遍历步骤
     */
    public static class TraversalStep {
        @JsonProperty("relationshipType")
        private String relationshipType; // 关系类型，* 表示任意类型

        @JsonProperty("direction")
        private String direction = "OUTGOING"; // INCOMING, OUTGOING, BOTH

        @JsonProperty("minHops")
        private Integer minHops = 1;

        @JsonProperty("maxHops")
        private Integer maxHops = 1;

        public TraversalStep() {}

        public TraversalStep(String relationshipType, String direction) {
            this.relationshipType = relationshipType;
            this.direction = direction;
        }

        public String getRelationshipType() {
            return relationshipType;
        }

        public void setRelationshipType(String relationshipType) {
            this.relationshipType = relationshipType;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public Integer getMinHops() {
            return minHops;
        }

        public void setMinHops(Integer minHops) {
            this.minHops = minHops;
        }

        public Integer getMaxHops() {
            return maxHops;
        }

        public void setMaxHops(Integer maxHops) {
            this.maxHops = maxHops;
        }
    }

    /**
     * 查询过滤器
     */
    public static class QueryFilter {
        @JsonProperty("target")
        private String target; // node, relationship

        @JsonProperty("property")
        private String property;

        @JsonProperty("operator")
        private String operator; // eq, ne, gt, lt, gte, lte, contains, in, exists

        @JsonProperty("value")
        private Object value;

        public QueryFilter() {}

        public QueryFilter(String target, String property, String operator, Object value) {
            this.target = target;
            this.property = property;
            this.operator = operator;
            this.value = value;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    /**
     * 返回结果定义
     */
    public static class ReturnDefinition {
        @JsonProperty("includeNodes")
        private boolean includeNodes = true;

        @JsonProperty("includeRelationships")
        private boolean includeRelationships = true;

        @JsonProperty("nodeProperties")
        private List<String> nodeProperties;

        @JsonProperty("relationshipProperties")
        private List<String> relationshipProperties;

        public ReturnDefinition() {}

        public boolean isIncludeNodes() {
            return includeNodes;
        }

        public void setIncludeNodes(boolean includeNodes) {
            this.includeNodes = includeNodes;
        }

        public boolean isIncludeRelationships() {
            return includeRelationships;
        }

        public void setIncludeRelationships(boolean includeRelationships) {
            this.includeRelationships = includeRelationships;
        }

        public List<String> getNodeProperties() {
            return nodeProperties;
        }

        public void setNodeProperties(List<String> nodeProperties) {
            this.nodeProperties = nodeProperties;
        }

        public List<String> getRelationshipProperties() {
            return relationshipProperties;
        }

        public void setRelationshipProperties(List<String> relationshipProperties) {
            this.relationshipProperties = relationshipProperties;
        }
    }

    public GraphQueryRequest() {}

    public List<NodeFilter> getStartNodes() {
        return startNodes;
    }

    public void setStartNodes(List<NodeFilter> startNodes) {
        this.startNodes = startNodes;
    }

    public List<TraversalStep> getTraversals() {
        return traversals;
    }

    public void setTraversals(List<TraversalStep> traversals) {
        this.traversals = traversals;
    }

    public List<QueryFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<QueryFilter> filters) {
        this.filters = filters;
    }

    public ReturnDefinition getReturnDefinition() {
        return returnDefinition;
    }

    public void setReturnDefinition(ReturnDefinition returnDefinition) {
        this.returnDefinition = returnDefinition;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        return "GraphQueryRequest{" +
                "startNodes=" + startNodes +
                ", traversals=" + traversals +
                ", filters=" + filters +
                ", returnDefinition=" + returnDefinition +
                ", limit=" + limit +
                '}';
    }
}