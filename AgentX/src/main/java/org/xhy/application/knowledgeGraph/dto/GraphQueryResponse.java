package org.xhy.application.knowledgeGraph.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 图查询响应DTO 用于返回图查询的结果数据
 * 
 * @author zang */
public class GraphQueryResponse {

    /** 查询是否成功 */
    private boolean success;

    /** 响应消息 */
    private String message;

    /** 查询结果节点列表 */
    private List<NodeResult> nodes;

    /** 查询结果关系列表 */
    private List<RelationshipResult> relationships;

    /** 查询结果总数 */
    private int totalCount;

    /** 查询执行时间 */
    private LocalDateTime executedAt;

    /** 查询耗时（毫秒） */
    private long executionTimeMs;

    /** 节点结果 */
    public static class NodeResult {
        private String id;
        private List<String> labels;
        private Map<String, Object> properties;

        public NodeResult() {
        }

        public NodeResult(String id, List<String> labels, Map<String, Object> properties) {
            this.id = id;
            this.labels = labels;
            this.properties = properties;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<String> getLabels() {
            return labels;
        }

        public void setLabels(List<String> labels) {
            this.labels = labels;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }

        @Override
        public String toString() {
            return "NodeResult{" + "id='" + id + '\'' + ", labels=" + labels + ", properties=" + properties + '}';
        }
    }

    /** 关系结果 */
    public static class RelationshipResult {
        private Long id;
        private String type;
        private String sourceNodeId;
        private String targetNodeId;
        private Map<String, Object> properties;

        public RelationshipResult() {
        }

        public RelationshipResult(Long id, String type, String sourceNodeId, String targetNodeId,
                Map<String, Object> properties) {
            this.id = id;
            this.type = type;
            this.sourceNodeId = sourceNodeId;
            this.targetNodeId = targetNodeId;
            this.properties = properties;
        }

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

        public String getSourceNodeId() {
            return sourceNodeId;
        }

        public void setSourceNodeId(String sourceNodeId) {
            this.sourceNodeId = sourceNodeId;
        }

        public String getTargetNodeId() {
            return targetNodeId;
        }

        public void setTargetNodeId(String targetNodeId) {
            this.targetNodeId = targetNodeId;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }

        @Override
        public String toString() {
            return "RelationshipResult{" + "id=" + id + ", type='" + type + '\'' + ", sourceNodeId='" + sourceNodeId
                    + '\'' + ", targetNodeId='" + targetNodeId + '\'' + ", properties=" + properties + '}';
        }
    }

    public GraphQueryResponse() {
        this.executedAt = LocalDateTime.now();
    }

    public GraphQueryResponse(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    /** 创建成功响应 */
    public static GraphQueryResponse success(List<NodeResult> nodes, List<RelationshipResult> relationships) {
        GraphQueryResponse response = new GraphQueryResponse(true, "查询成功");
        response.setNodes(nodes);
        response.setRelationships(relationships);
        response.setTotalCount((nodes != null ? nodes.size() : 0) + (relationships != null ? relationships.size() : 0));
        return response;
    }

    /** 创建失败响应 */
    public static GraphQueryResponse failure(String message) {
        return new GraphQueryResponse(false, message);
    }

    /** 设置执行耗时 */
    public void setExecutionTime(long startTimeMs) {
        this.executionTimeMs = System.currentTimeMillis() - startTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<NodeResult> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeResult> nodes) {
        this.nodes = nodes;
    }

    public List<RelationshipResult> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<RelationshipResult> relationships) {
        this.relationships = relationships;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    @Override
    public String toString() {
        return "GraphQueryResponse{" + "success=" + success + ", message='" + message + '\'' + ", nodes="
                + (nodes != null ? nodes.size() : 0) + " items" + ", relationships="
                + (relationships != null ? relationships.size() : 0) + " items" + ", totalCount=" + totalCount
                + ", executedAt=" + executedAt + ", executionTimeMs=" + executionTimeMs + '}';
    }
}