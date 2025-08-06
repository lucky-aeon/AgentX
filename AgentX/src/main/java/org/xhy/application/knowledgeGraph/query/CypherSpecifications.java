package org.xhy.application.knowledgeGraph.query;

import org.xhy.application.knowledgeGraph.dto.GraphQueryRequest;

/**
 * Cypher规约工厂类
 * 提供创建各种规约实例的静态方法
 * 
 * @author zang
 */
public class CypherSpecifications {

    /**
     * 创建属性等值规约
     */
    public static CypherSpecification propertyEquals(String property, Object value) {
        return new PropertyEqualsSpecification(property, value);
    }

    /**
     * 创建属性包含规约
     */
    public static CypherSpecification propertyContains(String property, String value) {
        return new PropertyContainsSpecification(property, value);
    }

    /**
     * 创建属性IN列表规约
     */
    public static CypherSpecification propertyIn(String property, Object... values) {
        return new PropertyInListSpecification(property, values);
    }

    /**
     * 创建关系存在性规约
     */
    public static CypherSpecification relationshipExists(String relationshipType, String direction) {
        return new RelationshipExistsSpecification(relationshipType, direction);
    }

    /**
     * 创建数值比较规约（大于）
     */
    public static CypherSpecification propertyGreaterThan(String property, Object value) {
        return new PropertyComparisonSpecification(property, value, ">");
    }

    /**
     * 创建数值比较规约（小于）
     */
    public static CypherSpecification propertyLessThan(String property, Object value) {
        return new PropertyComparisonSpecification(property, value, "<");
    }

    /**
     * 创建数值比较规约（大于等于）
     */
    public static CypherSpecification propertyGreaterThanOrEqual(String property, Object value) {
        return new PropertyComparisonSpecification(property, value, ">=");
    }

    /**
     * 创建数值比较规约（小于等于）
     */
    public static CypherSpecification propertyLessThanOrEqual(String property, Object value) {
        return new PropertyComparisonSpecification(property, value, "<=");
    }

    /**
     * 创建数值比较规约（不等于）
     */
    public static CypherSpecification propertyNotEquals(String property, Object value) {
        return new PropertyComparisonSpecification(property, value, "<>");
    }

    /**
     * 根据查询过滤器创建规约
     */
    public static CypherSpecification fromQueryFilter(GraphQueryRequest.QueryFilter filter) {
        String operator = filter.getOperator().toLowerCase();
        String property = filter.getProperty();
        Object value = filter.getValue();

        switch (operator) {
            case "eq":
            case "equals":
                return propertyEquals(property, value);
            case "ne":
            case "not_equals":
                return propertyNotEquals(property, value);
            case "gt":
                return propertyGreaterThan(property, value);
            case "lt":
                return propertyLessThan(property, value);
            case "gte":
                return propertyGreaterThanOrEqual(property, value);
            case "lte":
                return propertyLessThanOrEqual(property, value);
            case "contains":
                return propertyContains(property, value.toString());
            case "in":
                if (value instanceof Object[]) {
                    return propertyIn(property, (Object[]) value);
                } else {
                    throw new IllegalArgumentException("IN操作符需要数组类型的值");
                }
            default:
                throw new IllegalArgumentException("不支持的操作符: " + operator);
        }
    }

    /**
     * 根据节点过滤器创建规约
     */
    public static CypherSpecification fromNodeFilter(GraphQueryRequest.NodeFilter filter) {
        String operator = filter.getOperator().toLowerCase();
        String property = filter.getProperty();
        Object value = filter.getValue();

        switch (operator) {
            case "eq":
            case "equals":
                return propertyEquals(property, value);
            case "contains":
                return propertyContains(property, value.toString());
            case "in":
                if (value instanceof Object[]) {
                    return propertyIn(property, (Object[]) value);
                } else {
                    throw new IllegalArgumentException("IN操作符需要数组类型的值");
                }
            default:
                throw new IllegalArgumentException("不支持的操作符: " + operator);
        }
    }

    // 私有构造函数，防止实例化
    private CypherSpecifications() {}
}