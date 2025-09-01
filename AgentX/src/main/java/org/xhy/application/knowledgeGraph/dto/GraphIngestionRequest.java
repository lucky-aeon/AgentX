package org.xhy.application.knowledgeGraph.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/** 图数据摄取请求DTO 用于接收批量图数据摄取请求
 * 
 * @author zang */
public class GraphIngestionRequest {

    @JsonProperty("documentId")
    @NotBlank(message = "文档ID不能为空")
    private String documentId;

    @JsonProperty("source")
    private String source;

    @JsonProperty("entities")
    @Valid
    private List<EntityDto> entities;

    @JsonProperty("relationships")
    @Valid
    private List<RelationshipDto> relationships;

    public GraphIngestionRequest() {
    }

    public GraphIngestionRequest(String documentId, List<EntityDto> entities, List<RelationshipDto> relationships) {
        this.documentId = documentId;
        this.entities = entities;
        this.relationships = relationships;
    }

    /** 实体DTO - 支持动态标签和属性，包含文档分页信息 */
    public static class EntityDto {
        @JsonProperty("id")
        @NotBlank(message = "实体ID不能为空")
        private String id;

        @JsonProperty("labels")
        @NotEmpty(message = "实体标签不能为空")
        private List<String> labels;

        @JsonProperty("properties")
        @NotNull(message = "实体属性不能为null")
        private Map<String, Object> properties;

        @JsonProperty("fileId")
        private String fileId;

        @JsonProperty("pageNumber")
        private Integer pageNumber;

        public EntityDto() {
        }

        public EntityDto(String id, List<String> labels, Map<String, Object> properties) {
            this.id = id;
            this.labels = labels;
            this.properties = properties;
        }

        public EntityDto(String id, List<String> labels, Map<String, Object> properties, String fileId,
                Integer pageNumber) {
            this.id = id;
            this.labels = labels;
            this.properties = properties;
            this.fileId = fileId;
            this.pageNumber = pageNumber;
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

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public Integer getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(Integer pageNumber) {
            this.pageNumber = pageNumber;
        }

        @Override
        public String toString() {
            return "EntityDto{" + "id='" + id + '\'' + ", labels=" + labels + ", properties=" + properties
                    + ", fileId='" + fileId + '\'' + ", pageNumber=" + pageNumber + '}';
        }
    }

    /** 关系DTO - 支持动态关系类型和属性，包含文档分页信息 */
    public static class RelationshipDto {
        @JsonProperty("sourceId")
        @NotBlank(message = "源节点ID不能为空")
        private String sourceId;

        @JsonProperty("targetId")
        @NotBlank(message = "目标节点ID不能为空")
        private String targetId;

        @JsonProperty("type")
        @NotBlank(message = "关系类型不能为空")
        private String type;

        @JsonProperty("properties")
        private Map<String, Object> properties;

        @JsonProperty("fileId")
        private String fileId;

        @JsonProperty("pageNumber")
        private Integer pageNumber;

        public RelationshipDto() {
        }

        public RelationshipDto(String sourceId, String targetId, String type, Map<String, Object> properties) {
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.type = type;
            this.properties = properties;
        }

        public RelationshipDto(String sourceId, String targetId, String type, Map<String, Object> properties,
                String fileId, Integer pageNumber) {
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.type = type;
            this.properties = properties;
            this.fileId = fileId;
            this.pageNumber = pageNumber;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
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
        }

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public Integer getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(Integer pageNumber) {
            this.pageNumber = pageNumber;
        }

        @Override
        public String toString() {
            return "RelationshipDto{" + "sourceId='" + sourceId + '\'' + ", targetId='" + targetId + '\'' + ", type='"
                    + type + '\'' + ", properties=" + properties + ", fileId='" + fileId + '\'' + ", pageNumber="
                    + pageNumber + '}';
        }
    }

    // Getters and Setters
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<EntityDto> getEntities() {
        return entities;
    }

    public void setEntities(List<EntityDto> entities) {
        this.entities = entities;
    }

    public List<RelationshipDto> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<RelationshipDto> relationships) {
        this.relationships = relationships;
    }

    @Override
    public String toString() {
        return "GraphIngestionRequest{" + "documentId='" + documentId + '\'' + ", source='" + source + '\''
                + ", entities=" + entities + ", relationships=" + relationships + '}';
    }
}