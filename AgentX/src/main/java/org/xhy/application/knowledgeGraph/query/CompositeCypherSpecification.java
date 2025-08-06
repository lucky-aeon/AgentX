package org.xhy.application.knowledgeGraph.query;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 组合Cypher规约实现
 * 用于将多个规约通过AND或OR逻辑组合
 * 
 * @author zang
 */
public class CompositeCypherSpecification implements CypherSpecification {

    private final CypherSpecification left;
    private final CypherSpecification right;
    private final String operator;

    public CompositeCypherSpecification(CypherSpecification left, CypherSpecification right, String operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public String toCypher(String alias) {
        String leftCypher = left.toCypher(alias);
        String rightCypher = right.toCypher(alias);
        return "(" + leftCypher + " " + operator + " " + rightCypher + ")";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.putAll(left.getParameters());
        parameters.putAll(right.getParameters());
        return parameters;
    }
}

/**
 * 属性等值规约实现
 */
class PropertyEqualsSpecification implements CypherSpecification {

    private final String property;
    private final Object value;
    private final String paramKey;

    public PropertyEqualsSpecification(String property, Object value) {
        this.property = property;
        this.value = value;
        this.paramKey = "param_" + property + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Override
    public String toCypher(String alias) {
        return alias + "." + property + " = $" + paramKey;
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(paramKey, value);
        return params;
    }
}

/**
 * 属性包含规约实现
 */
class PropertyContainsSpecification implements CypherSpecification {

    private final String property;
    private final String value;
    private final String paramKey;

    public PropertyContainsSpecification(String property, String value) {
        this.property = property;
        this.value = value;
        this.paramKey = "param_" + property + "_contains_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Override
    public String toCypher(String alias) {
        return alias + "." + property + " CONTAINS $" + paramKey;
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(paramKey, value);
        return params;
    }
}

/**
 * 属性IN列表规约实现
 */
class PropertyInListSpecification implements CypherSpecification {

    private final String property;
    private final Object[] values;
    private final String paramKey;

    public PropertyInListSpecification(String property, Object... values) {
        this.property = property;
        this.values = values;
        this.paramKey = "param_" + property + "_in_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Override
    public String toCypher(String alias) {
        return alias + "." + property + " IN $" + paramKey;
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(paramKey, values);
        return params;
    }
}

/**
 * 关系存在性规约实现
 */
class RelationshipExistsSpecification implements CypherSpecification {

    private final String relationshipType;
    private final String direction;
    private final String paramKey;

    public RelationshipExistsSpecification(String relationshipType, String direction) {
        this.relationshipType = relationshipType;
        this.direction = direction;
        this.paramKey = "param_rel_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Override
    public String toCypher(String alias) {
        String pattern;
        switch (direction.toUpperCase()) {
            case "INCOMING":
                pattern = "()<-[:" + relationshipType + "]-(" + alias + ")";
                break;
            case "OUTGOING":
                pattern = "(" + alias + ")-[:" + relationshipType + "]->()";
                break;
            case "BOTH":
                pattern = "(" + alias + ")-[:" + relationshipType + "]-()";
                break;
            default:
                throw new IllegalArgumentException("Unsupported direction: " + direction);
        }
        return "exists(" + pattern + ")";
    }

    @Override
    public Map<String, Object> getParameters() {
        return new HashMap<>();
    }
}

/**
 * 数值比较规约实现
 */
class PropertyComparisonSpecification implements CypherSpecification {

    private final String property;
    private final Object value;
    private final String operator;
    private final String paramKey;

    public PropertyComparisonSpecification(String property, Object value, String operator) {
        this.property = property;
        this.value = value;
        this.operator = operator;
        this.paramKey = "param_" + property + "_" + operator + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Override
    public String toCypher(String alias) {
        return alias + "." + property + " " + operator + " $" + paramKey;
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(paramKey, value);
        return params;
    }
}