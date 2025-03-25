package org.xhy.domain.conversation.service;

import org.springframework.stereotype.Service;
import org.xhy.application.conversation.assembler.SessionAssembler;
import org.xhy.domain.conversation.dto.SessionDTO;
import org.xhy.domain.conversation.model.SessionEntity;
import org.xhy.domain.conversation.repository.SessionRepository;
import org.xhy.infrastructure.exception.BusinessException;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SessionDomainService {

    private final SessionRepository sessionRepository;

    public SessionDomainService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * 根据 agentId 获取会话列表
     * 
     * @param agentId 助理id
     * @return
     */
    public List<SessionDTO> getSessionsByAgentId(String agentId) {
        List<SessionEntity> sessions = sessionRepository.selectList(Wrappers.<SessionEntity>lambdaQuery()
                .eq(SessionEntity::getAgentId, agentId).orderByDesc(SessionEntity::getCreatedAt));
        return sessions.stream().map(SessionAssembler::toDTO).collect(Collectors.toList());
    }

    /**
     * 删除会话
     * 
     * @param id
     * @param userId
     */
    public boolean deleteSession(String id, String userId) {
        return sessionRepository.delete(Wrappers.<SessionEntity>lambdaQuery()
                .eq(SessionEntity::getId, id).eq(SessionEntity::getUserId, userId)) > 0;
    }

    /**
     * 更新会话
     * 
     * @param id     会话id
     * @param userId 用户id
     * @param title  标题
     */
    public void updateSession(String id, String userId, String title) {
        SessionEntity session = new SessionEntity();
        session.setId(id);
        session.setUserId(userId);
        session.setTitle(title);
        sessionRepository.update(session, Wrappers.<SessionEntity>lambdaUpdate()
                .eq(SessionEntity::getId, id).eq(SessionEntity::getUserId, userId));
    }

    /**
     * 创建会话
     * 
     * @param agentId 助理id
     * @param userId  用户id
     * @return
     */
    public SessionDTO createSession(String agentId, String userId) {
        SessionEntity session = new SessionEntity();
        session.setAgentId(agentId);
        session.setUserId(userId);
        session.setTitle("新会话");
        sessionRepository.insert(session);
        return SessionAssembler.toDTO(session);
    }

    /**
     * 检查会话是否存在
     * 
     * @param id
     * @param userId
     * @return
     */
    public void checkSessionExist(String id, String userId) {
        SessionEntity session = sessionRepository.selectOne(Wrappers.<SessionEntity>lambdaQuery()
                .eq(SessionEntity::getId, id).eq(SessionEntity::getUserId, userId));
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
    }

    public SessionEntity find(String sessionId,String userId) {
        SessionEntity session = sessionRepository.selectOne(Wrappers.<SessionEntity>lambdaQuery()
                .eq(SessionEntity::getId, sessionId).eq(SessionEntity::getUserId, userId));

        return session;
    }

    public void deleteSessions(List<String> sessionIds) {
        sessionRepository.delete(Wrappers.<SessionEntity>lambdaQuery()
                .in(SessionEntity::getId, sessionIds));
    }
}
