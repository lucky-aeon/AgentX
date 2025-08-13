package org.xhy.domain.conversation.constant;

import org.xhy.domain.llm.model.enums.ModelType;
import org.xhy.infrastructure.exception.BusinessException;

public enum Role {

    USER, SYSTEM, ASSISTANT,
    /** 只会存在历史消息表中作为特殊消息存在，而实际发送给大模型的消息时会被转换对应的消息类型：AiMessage */
    SUMMARY;

    public static Role fromCode(String code) {
        for (Role role : values()) {
            if (role.name().equals(code)) {
                return role;
            }
        }
        throw new BusinessException("Unknown model type code: " + code);
    }
}
