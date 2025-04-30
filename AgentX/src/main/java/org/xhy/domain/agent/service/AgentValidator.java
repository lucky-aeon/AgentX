package org.xhy.domain.agent.service;

import org.springframework.stereotype.Component;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.conversation.model.SessionEntity;
import org.xhy.domain.conversation.service.SessionDomainService;
import org.xhy.infrastructure.exception.BusinessException;

/** Agent验证器 负责验证Agent的可用性 */
@Component
public class AgentValidator {

    private final SessionDomainService sessionDomainService;
    private final AgentDomainService agentDomainService;

    public AgentValidator(SessionDomainService sessionDomainService, AgentDomainService agentDomainService) {
        this.sessionDomainService = sessionDomainService;
        this.agentDomainService = agentDomainService;
    }

    /** 验证会话和Agent
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 验证结果 */
    public ValidationResult validateSessionAndAgent(String sessionId, String userId) {
        // 获取会话
        SessionEntity session = sessionDomainService.getSession(sessionId, userId);
        String agentId = session.getAgentId();

        // 获取对应agent是否可以使用：如果 userId 不同并且是禁用，则不可对话
        AgentEntity agent = agentDomainService.getAgentById(agentId);
        if (!agent.getUserId().equals(userId) && !agent.getEnabled()) {
            throw new BusinessException("agent已被禁用");
        }

        return new ValidationResult(session, agent);
    }

    /** 验证结果 */
    public static class ValidationResult {
        private final SessionEntity sessionEntity;
        private final AgentEntity agentEntity;

        public ValidationResult(SessionEntity sessionEntity, AgentEntity agentEntity) {
            this.sessionEntity = sessionEntity;
            this.agentEntity = agentEntity;
        }

        public SessionEntity getSessionEntity() {
            return sessionEntity;
        }

        public AgentEntity getAgentEntity() {
            return agentEntity;
        }
    }
}
