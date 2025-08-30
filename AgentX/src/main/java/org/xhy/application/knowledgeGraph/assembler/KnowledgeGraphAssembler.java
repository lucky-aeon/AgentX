package org.xhy.application.knowledgeGraph.assembler;

import org.springframework.beans.BeanUtils;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.application.knowledgeGraph.dto.GraphQueryResponse;
import org.xhy.domain.knowledgeGraph.entity.GenericNode;
import org.xhy.domain.knowledgeGraph.entity.GenericRelationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 知识图谱对象转换器
 * 负责处理知识图谱相关的DTO、Entity之间的转换
 * 使用BeanUtils.copyProperties()确保字段复制的完整性
 * 
 * @author zang
 * @since 1.0.0
 */
public class KnowledgeGraphAssembler {

    /**
     * 将实体DTO转换为GenericNode实体
     * 
     * @param entityDto 实体DTO
     * @return GenericNode实体，如果输入为null则返回null
     */
    public static GenericNode toGenericNode(GraphIngestionRequest.EntityDto entityDto) {
        if (entityDto == null) {
            return null;
        }

        GenericNode node = new GenericNode();
        node.setId(entityDto.getId());
        
        // 复制标签
        if (entityDto.getLabels() != null) {
            entityDto.getLabels().forEach(node::addLabel);
        }
        
        // 复制属性
        if (entityDto.getProperties() != null) {
            node.setProperties(entityDto.getProperties());
        }

        return node;
    }

    /**
     * 将GenericNode实体转换为节点结果DTO
     * 
     * @param node GenericNode实体
     * @return 节点结果DTO，如果输入为null则返回null
     */
    public static GraphQueryResponse.NodeResult toNodeResult(GenericNode node) {
        if (node == null) {
            return null;
        }

        return new GraphQueryResponse.NodeResult(
                node.getId(),
                node.getLabels() != null ? new ArrayList<>(node.getLabels()) : new ArrayList<>(),
                node.getProperties()
        );
    }

    /**
     * 将关系DTO转换为GenericRelationship实体
     * 
     * @param relationshipDto 关系DTO
     * @return GenericRelationship实体，如果输入为null则返回null
     */
    public static GenericRelationship toGenericRelationship(GraphIngestionRequest.RelationshipDto relationshipDto) {
        if (relationshipDto == null) {
            return null;
        }

        GenericRelationship relationship = new GenericRelationship();
        relationship.setType(relationshipDto.getType());
        
        // 复制属性
        if (relationshipDto.getProperties() != null) {
            relationship.setProperties(relationshipDto.getProperties());
        }

        return relationship;
    }

    /**
     * 将GenericRelationship实体转换为关系结果DTO
     * 
     * @param relationship GenericRelationship实体
     * @param sourceNodeId 源节点ID
     * @param targetNodeId 目标节点ID
     * @return 关系结果DTO，如果输入为null则返回null
     */
    public static GraphQueryResponse.RelationshipResult toRelationshipResult(
            GenericRelationship relationship, String sourceNodeId, String targetNodeId) {
        if (relationship == null) {
            return null;
        }

        return new GraphQueryResponse.RelationshipResult(
                relationship.getId(),
                relationship.getType(),
                sourceNodeId,
                targetNodeId,
                relationship.getProperties()
        );
    }

    /**
     * 将实体DTO列表转换为GenericNode实体列表
     * 
     * @param entityDtos 实体DTO列表
     * @return GenericNode实体列表，如果输入为null或空则返回空列表
     */
    public static List<GenericNode> toGenericNodes(List<GraphIngestionRequest.EntityDto> entityDtos) {
        if (entityDtos == null || entityDtos.isEmpty()) {
            return Collections.emptyList();
        }

        return entityDtos.stream()
                .map(KnowledgeGraphAssembler::toGenericNode)
                .toList();
    }

    /**
     * 将GenericNode实体列表转换为节点结果DTO列表
     * 
     * @param nodes GenericNode实体列表
     * @return 节点结果DTO列表，如果输入为null或空则返回空列表
     */
    public static List<GraphQueryResponse.NodeResult> toNodeResults(List<GenericNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        return nodes.stream()
                .map(KnowledgeGraphAssembler::toNodeResult)
                .toList();
    }

    /**
     * 将关系DTO列表转换为GenericRelationship实体列表
     * 
     * @param relationshipDtos 关系DTO列表
     * @return GenericRelationship实体列表，如果输入为null或空则返回空列表
     */
    public static List<GenericRelationship> toGenericRelationships(
            List<GraphIngestionRequest.RelationshipDto> relationshipDtos) {
        if (relationshipDtos == null || relationshipDtos.isEmpty()) {
            return Collections.emptyList();
        }

        return relationshipDtos.stream()
                .map(KnowledgeGraphAssembler::toGenericRelationship)
                .toList();
    }

    /**
     * 创建节点结果DTO
     * 便捷方法用于直接创建节点结果
     * 
     * @param id 节点ID
     * @param labels 标签列表
     * @param properties 属性映射
     * @return 节点结果DTO
     */
    public static GraphQueryResponse.NodeResult createNodeResult(
            String id, List<String> labels, Map<String, Object> properties) {
        return new GraphQueryResponse.NodeResult(
                id,
                labels != null ? labels : new ArrayList<>(),
                properties
        );
    }

    /**
     * 创建关系结果DTO
     * 便捷方法用于直接创建关系结果
     * 
     * @param id 关系ID
     * @param type 关系类型
     * @param sourceNodeId 源节点ID
     * @param targetNodeId 目标节点ID
     * @param properties 属性映射
     * @return 关系结果DTO
     */
    public static GraphQueryResponse.RelationshipResult createRelationshipResult(
            Long id, String type, String sourceNodeId, String targetNodeId, Map<String, Object> properties) {
        return new GraphQueryResponse.RelationshipResult(
                id,
                type,
                sourceNodeId,
                targetNodeId,
                properties
        );
    }

    /**
     * 复制节点属性
     * 使用BeanUtils复制通用属性，然后处理特殊字段
     * 
     * @param source 源节点
     * @param target 目标节点
     */
    public static void copyNodeProperties(GenericNode source, GenericNode target) {
        if (source == null || target == null) {
            return;
        }

        // 复制基本属性
        target.setId(source.getId());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());

        // 复制标签集合
        if (source.getLabels() != null) {
            target.getLabels().clear();
            source.getLabels().forEach(target::addLabel);
        }

        // 复制属性映射
        if (source.getProperties() != null) {
            target.setProperties(source.getProperties());
        }
    }

    /**
     * 复制关系属性
     * 使用BeanUtils复制通用属性，然后处理特殊字段
     * 
     * @param source 源关系
     * @param target 目标关系
     */
    public static void copyRelationshipProperties(GenericRelationship source, GenericRelationship target) {
        if (source == null || target == null) {
            return;
        }

        // 复制基本属性
        target.setId(source.getId());
        target.setType(source.getType());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());

        // 复制属性映射
        if (source.getProperties() != null) {
            target.setProperties(source.getProperties());
        }

        // 复制目标节点引用
        target.setTargetNode(source.getTargetNode());
    }
}
