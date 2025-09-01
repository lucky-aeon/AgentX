package org.xhy.application.knowledgeGraph.query;

import java.util.Map;

/** Cypher规约接口 实现规约模式（Specification Pattern）用于构建动态、可组合的Cypher查询条件
 * 
 * @author zang */
public interface CypherSpecification {

    /** 生成Cypher查询片段
     * 
     * @param alias 节点或关系的别名
     * @return Cypher查询片段，例如: "alias.name = $nameParam" */
    String toCypher(String alias);

    /** 获取该规约所需的参数映射
     * 
     * @return 参数映射，例如: {"nameParam": "胡展鸿"} */
    Map<String, Object> getParameters();

    /** 组合规约 - AND逻辑
     * 
     * @param other 另一个规约
     * @return 组合后的规约 */
    default CypherSpecification and(CypherSpecification other) {
        return new CompositeCypherSpecification(this, other, "AND");
    }

    /** 组合规约 - OR逻辑
     * 
     * @param other 另一个规约
     * @return 组合后的规约 */
    default CypherSpecification or(CypherSpecification other) {
        return new CompositeCypherSpecification(this, other, "OR");
    }
}