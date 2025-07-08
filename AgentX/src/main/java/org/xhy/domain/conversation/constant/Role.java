package org.xhy.domain.conversation.constant;

import org.xhy.domain.llm.model.enums.ModelType;
import org.xhy.infrastructure.exception.BusinessException;

public enum Role {

    USER, SYSTEM, ASSISTANT;

    public static Role fromCode(String code) {
        for (Role role : values()) {
            if (role.name().equals(code)) {
                return role;
            }
        }
        throw new BusinessException("Unknown model type code: " + code);
    }
}
