package org.xhy.infrastructure.neo4j.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Neo4j值类型转换器
 * 将Java类型转换为Neo4j支持的类型，特别处理BigDecimal等不支持的类型
 * 
 * @author AgentX
 */
public class Neo4jValueConverter {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jValueConverter.class);

    /**
     * 转换Map中的值为Neo4j支持的类型
     * 
     * @param properties 原始属性Map
     * @return 转换后的属性Map
     */
    public static Map<String, Object> convertProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }

        Map<String, Object> convertedProperties = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Object convertedValue = convertValue(value);
            
            convertedProperties.put(key, convertedValue);
        }
        
        return convertedProperties;
    }

    /**
     * 转换单个值为Neo4j支持的类型
     * 
     * @param value 原始值
     * @return 转换后的值
     */
    @SuppressWarnings("unchecked")
    public static Object convertValue(Object value) {
        if (value == null) {
            return null;
        }

        // BigDecimal -> Double
        if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) value;
            Double converted = decimal.doubleValue();
            logger.debug("转换BigDecimal {} 为 Double {}", decimal, converted);
            return converted;
        }

        // List递归转换
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            return list.stream()
                    .map(Neo4jValueConverter::convertValue)
                    .collect(Collectors.toList());
        }

        // Map递归转换
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            return convertProperties(map);
        }

        // 其他支持的类型直接返回
        // Neo4j支持：String, Long, Double, Boolean, 数组和List
        if (value instanceof String || 
            value instanceof Long || 
            value instanceof Integer ||
            value instanceof Double || 
            value instanceof Float ||
            value instanceof Boolean ||
            value instanceof String[] ||
            value instanceof long[] ||
            value instanceof double[] ||
            value instanceof boolean[]) {
            return value;
        }

        // Float -> Double
        if (value instanceof Float) {
            return ((Float) value).doubleValue();
        }

        // Integer -> Long (Neo4j更偏向Long)
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        // 未知类型转为String
        logger.warn("未知类型 {} 将被转换为String: {}", value.getClass().getSimpleName(), value);
        return value.toString();
    }

    /**
     * 检查值是否为Neo4j支持的类型
     * 
     * @param value 要检查的值
     * @return 是否支持
     */
    public static boolean isNeo4jSupportedType(Object value) {
        if (value == null) {
            return true;
        }

        return value instanceof String || 
               value instanceof Long || 
               value instanceof Double || 
               value instanceof Boolean ||
               value instanceof String[] ||
               value instanceof long[] ||
               value instanceof double[] ||
               value instanceof boolean[] ||
               value instanceof List ||
               value instanceof Map;
    }

    /**
     * 批量转换实体列表的属性
     * 
     * @param entities 实体列表
     * @return 转换后的实体参数列表
     */
    public static List<Map<String, Object>> convertEntityList(List<Map<String, Object>> entities) {
        if (entities == null || entities.isEmpty()) {
            return entities;
        }

        return entities.stream()
                .map(entity -> {
                    Map<String, Object> convertedEntity = new HashMap<>(entity);
                    
                    // 转换properties字段
                    if (entity.containsKey("properties")) {
                        Object properties = entity.get("properties");
                        if (properties instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> propertiesMap = (Map<String, Object>) properties;
                            convertedEntity.put("properties", convertProperties(propertiesMap));
                        }
                    }
                    
                    return convertedEntity;
                })
                .collect(Collectors.toList());
    }

    /**
     * 批量转换关系列表的属性
     * 
     * @param relationships 关系列表
     * @return 转换后的关系参数列表
     */
    public static List<Map<String, Object>> convertRelationshipList(List<Map<String, Object>> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return relationships;
        }

        return relationships.stream()
                .map(relationship -> {
                    Map<String, Object> convertedRelationship = new HashMap<>(relationship);
                    
                    // 转换properties字段
                    if (relationship.containsKey("properties")) {
                        Object properties = relationship.get("properties");
                        if (properties instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> propertiesMap = (Map<String, Object>) properties;
                            convertedRelationship.put("properties", convertProperties(propertiesMap));
                        }
                    }
                    
                    return convertedRelationship;
                })
                .collect(Collectors.toList());
    }
}
