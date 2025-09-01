package org.xhy.application.knowledgeGraph.query;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 属性等值规约实现
 * 
 * @author zang */
public class PropertyEqualsSpecification implements CypherSpecification {

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
