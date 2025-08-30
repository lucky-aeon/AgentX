package org.xhy.application.knowledgeGraph.query;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 属性包含规约实现
 * 
 * @author zang
 */
public class PropertyContainsSpecification implements CypherSpecification {

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
