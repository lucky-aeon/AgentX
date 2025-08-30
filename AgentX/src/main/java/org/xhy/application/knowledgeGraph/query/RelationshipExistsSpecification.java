package org.xhy.application.knowledgeGraph.query;

import java.util.HashMap;
import java.util.Map;

/**
 * 关系存在性规约实现
 * 
 * @author zang
 */
public class RelationshipExistsSpecification implements CypherSpecification {

    private final String relationshipType;
    private final String direction;

    public RelationshipExistsSpecification(String relationshipType, String direction) {
        this.relationshipType = relationshipType;
        this.direction = direction;
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
