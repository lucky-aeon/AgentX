package org.xhy.application.knowledgeGraph.query;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 属性IN列表规约实现
 * 
 * @author zang */
public class PropertyInListSpecification implements CypherSpecification {

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
