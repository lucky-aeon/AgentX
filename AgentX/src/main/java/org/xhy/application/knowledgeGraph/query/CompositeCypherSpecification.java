package org.xhy.application.knowledgeGraph.query;

import java.util.HashMap;
import java.util.Map;

/** 组合Cypher规约实现 用于将多个规约通过AND或OR逻辑组合
 * 
 * @author zang */
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