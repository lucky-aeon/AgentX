package org.xhy.infrastructure.llm.protocol.enums;

import org.xhy.domain.conversation.constant.Role;
import org.xhy.infrastructure.exception.BusinessException;

public enum ProviderProtocol {

    OpenAI,
    CLAUDE;

    public static ProviderProtocol fromCode(String code) {
        for (ProviderProtocol protocol : values()) {
            if (protocol.name().equals(code)) {
                return protocol;
            }
        }
        throw new BusinessException("Unknown model type code: " + code);
    }
}
