package org.xhy.application.knowledgeGraph.query;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 数值比较规约实现
 * 
 * @author zang
 */
public class PropertyComparisonSpecification implements CypherSpecification {

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
