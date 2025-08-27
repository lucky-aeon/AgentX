package org.xhy.domain.knowledgeGraph.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest.EntityDto;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest.RelationshipDto;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 实体冲突检测和合并服务
 * 负责处理分页知识图谱提取中的实体冲突问题
 * 
 * @author shilong.zang
 */
@Service
public class EntityConflictResolutionService {

    private static final Logger logger = LoggerFactory.getLogger(EntityConflictResolutionService.class);

    /**
     * 合并同一文档中的冲突实体
     * 
     * @param entities 实体列表
     * @return 合并后的实体列表
     */
    public List<EntityDto> mergeConflictingEntities(List<EntityDto> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        logger.info("开始合并冲突实体，输入实体数量: {}", entities.size());

        // 按实体ID分组
        Map<String, List<EntityDto>> entitiesById = entities.stream()
                .collect(Collectors.groupingBy(EntityDto::getId));

        List<EntityDto> mergedEntities = new ArrayList<>();

        for (Map.Entry<String, List<EntityDto>> entry : entitiesById.entrySet()) {
            String entityId = entry.getKey();
            List<EntityDto> conflictingEntities = entry.getValue();

            if (conflictingEntities.size() == 1) {
                // 无冲突，直接添加
                mergedEntities.add(conflictingEntities.get(0));
            } else {
                // 有冲突，进行合并
                EntityDto mergedEntity = mergeEntityInstances(entityId, conflictingEntities);
                mergedEntities.add(mergedEntity);
                logger.info("合并实体 {} 的 {} 个实例", entityId, conflictingEntities.size());
            }
        }

        logger.info("实体合并完成，输出实体数量: {}", mergedEntities.size());
        return mergedEntities;
    }

    /**
     * 合并同一实体的多个实例
     * 
     * @param entityId 实体ID
     * @param instances 实体实例列表
     * @return 合并后的实体
     */
    private EntityDto mergeEntityInstances(String entityId, List<EntityDto> instances) {
        if (instances.isEmpty()) {
            throw new IllegalArgumentException("实体实例列表不能为空");
        }

        if (instances.size() == 1) {
            return instances.get(0);
        }

        EntityDto baseEntity = instances.get(0);
        
        // 合并标签（去重）
        Set<String> mergedLabels = new LinkedHashSet<>(baseEntity.getLabels());
        for (EntityDto instance : instances.subList(1, instances.size())) {
            if (instance.getLabels() != null) {
                mergedLabels.addAll(instance.getLabels());
            }
        }

        // 合并属性
        Map<String, Object> mergedProperties = new HashMap<>();
        if (baseEntity.getProperties() != null) {
            mergedProperties.putAll(baseEntity.getProperties());
        }

        for (EntityDto instance : instances.subList(1, instances.size())) {
            if (instance.getProperties() != null) {
                for (Map.Entry<String, Object> property : instance.getProperties().entrySet()) {
                    String key = property.getKey();
                    Object value = property.getValue();
                    
                    if (mergedProperties.containsKey(key)) {
                        // 属性冲突，使用合并策略
                        Object mergedValue = mergePropertyValues(key, mergedProperties.get(key), value);
                        mergedProperties.put(key, mergedValue);
                    } else {
                        mergedProperties.put(key, value);
                    }
                }
            }
        }

        // 收集来源页面信息
        List<Integer> sourcePages = instances.stream()
                .map(EntityDto::getPageNumber)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // 创建合并后的实体
        EntityDto mergedEntity = new EntityDto(
                entityId,
                new ArrayList<>(mergedLabels),
                mergedProperties,
                baseEntity.getFileId(),
                null // 合并实体不特定于某一页
        );

        // 在属性中记录来源页面
        mergedEntity.getProperties().put("sourcePages", sourcePages);
        mergedEntity.getProperties().put("mergedFromPages", sourcePages.size());

        return mergedEntity;
    }

    /**
     * 合并属性值的策略
     * 
     * @param key 属性键
     * @param existingValue 已存在的值
     * @param newValue 新值
     * @return 合并后的值
     */
    private Object mergePropertyValues(String key, Object existingValue, Object newValue) {
        if (existingValue == null) {
            return newValue;
        }
        
        if (newValue == null) {
            return existingValue;
        }

        // 如果值相同，直接返回
        if (existingValue.equals(newValue)) {
            return existingValue;
        }

        // 特殊属性的合并策略
        switch (key) {
            case "description":
            case "content":
            case "text":
                // 文本类属性：连接文本
                return mergeTextValues(existingValue.toString(), newValue.toString());
                
            case "confidence":
            case "score":
                // 数值类属性：取平均值或最大值
                return mergeNumericValues(existingValue, newValue);
                
            default:
                // 默认策略：创建值列表
                return createValueList(existingValue, newValue);
        }
    }

    /**
     * 合并文本值
     */
    private String mergeTextValues(String existing, String newValue) {
        if (existing.equals(newValue)) {
            return existing;
        }
        
        // 避免重复内容
        if (existing.contains(newValue)) {
            return existing;
        }
        
        if (newValue.contains(existing)) {
            return newValue;
        }
        
        // 连接不同的文本内容
        return existing + "; " + newValue;
    }

    /**
     * 合并数值
     */
    private Object mergeNumericValues(Object existing, Object newValue) {
        try {
            double existingNum = Double.parseDouble(existing.toString());
            double newNum = Double.parseDouble(newValue.toString());
            
            // 取较大值（可根据业务需求调整为平均值）
            return Math.max(existingNum, newNum);
        } catch (NumberFormatException e) {
            // 不是数值，使用默认策略
            return createValueList(existing, newValue);
        }
    }

    /**
     * 创建值列表
     */
    private List<Object> createValueList(Object existing, Object newValue) {
        List<Object> values = new ArrayList<>();
        
        if (existing instanceof List) {
            values.addAll((List<?>) existing);
        } else {
            values.add(existing);
        }
        
        if (newValue instanceof List) {
            values.addAll((List<?>) newValue);
        } else {
            values.add(newValue);
        }
        
        // 去重
        return values.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 更新关系以使用合并后的实体ID
     * 
     * @param relationships 关系列表
     * @param entityMergeMap 实体合并映射（原ID -> 合并后ID）
     * @return 更新后的关系列表
     */
    public List<RelationshipDto> updateRelationshipsAfterEntityMerge(
            List<RelationshipDto> relationships, 
            Map<String, String> entityMergeMap) {
        
        if (relationships == null || relationships.isEmpty()) {
            return new ArrayList<>();
        }

        return relationships.stream()
                .map(rel -> updateRelationshipEntityReferences(rel, entityMergeMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 更新单个关系的实体引用
     */
    private RelationshipDto updateRelationshipEntityReferences(
            RelationshipDto relationship, 
            Map<String, String> entityMergeMap) {
        
        String sourceId = relationship.getSourceId();
        String targetId = relationship.getTargetId();
        
        // 更新源实体ID
        String updatedSourceId = entityMergeMap.getOrDefault(sourceId, sourceId);
        
        // 更新目标实体ID
        String updatedTargetId = entityMergeMap.getOrDefault(targetId, targetId);
        
        // 检查是否为自循环关系（合并后源和目标相同）
        if (updatedSourceId.equals(updatedTargetId)) {
            logger.debug("跳过自循环关系: {} -> {}", sourceId, targetId);
            return null; // 过滤掉自循环关系
        }
        
        // 创建更新后的关系
        RelationshipDto updatedRelationship = new RelationshipDto(
                updatedSourceId,
                updatedTargetId,
                relationship.getType(),
                relationship.getProperties(),
                relationship.getFileId(),
                relationship.getPageNumber()
        );
        
        return updatedRelationship;
    }

    /**
     * 生成实体合并映射
     * 用于记录哪些实体被合并了
     * 
     * @param originalEntities 原始实体列表
     * @param mergedEntities 合并后实体列表
     * @return 合并映射
     */
    public Map<String, String> generateEntityMergeMap(
            List<EntityDto> originalEntities, 
            List<EntityDto> mergedEntities) {
        
        Map<String, String> mergeMap = new HashMap<>();
        
        // 对于合并后的实体，所有原始实体ID都映射到合并后的ID
        for (EntityDto mergedEntity : mergedEntities) {
            String mergedId = mergedEntity.getId();
            
            // 查找所有具有相同ID的原始实体
            originalEntities.stream()
                    .filter(original -> original.getId().equals(mergedId))
                    .forEach(original -> mergeMap.put(original.getId(), mergedId));
        }
        
        return mergeMap;
    }
}
