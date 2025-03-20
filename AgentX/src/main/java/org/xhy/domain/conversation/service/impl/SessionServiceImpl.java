package org.xhy.domain.conversation.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.common.exception.EntityNotFoundException;
import org.xhy.domain.conversation.model.Session;
import org.xhy.domain.conversation.model.SessionDTO;
import org.xhy.domain.conversation.repository.SessionRepository;
import org.xhy.domain.conversation.service.ContextService;
import org.xhy.domain.conversation.service.MessageService;
import org.xhy.domain.conversation.service.SessionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

/**
 * 会话服务实现
 */
@Service
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final MessageService messageService;
    private final ContextService contextService;

    public SessionServiceImpl(SessionRepository sessionRepository, MessageService messageService,
            ContextService contextService) {
        this.sessionRepository = sessionRepository;
        this.messageService = messageService;
        this.contextService = contextService;
    }

    @Override
    @Transactional
    public SessionDTO createSession(String title, String userId, String description) {
        Session session = Session.createNew(title, userId);
        session.setDescription(description);
        sessionRepository.insert(session);

        // 创建会话初始上下文
        contextService.createInitialContext(session.getId());

        return session.toDTO();
    }

    @Override
    public SessionDTO getSession(String sessionId) {
        Session session = sessionRepository.selectById(sessionId);
        if (session == null) {
            throw new EntityNotFoundException("会话不存在: " + sessionId);
        }
        return session.toDTO();
    }

    @Override
    public List<SessionDTO> getUserSessions(String userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(Session::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SessionDTO> getUserActiveSessions(String userId) {
        return sessionRepository.findByUserIdAndIsArchivedOrderByUpdatedAtDesc(userId, false)
                .stream()
                .map(Session::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SessionDTO> getUserArchivedSessions(String userId) {
        return sessionRepository.findByUserIdAndIsArchivedOrderByUpdatedAtDesc(userId, true)
                .stream()
                .map(Session::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SessionDTO updateSession(String sessionId, String title, String description) {
        Session session = sessionRepository.selectById(sessionId);
        if (session == null) {
            throw new EntityNotFoundException("会话不存在: " + sessionId);
        }

        session.update(title, description);
        sessionRepository.updateById(session);

        return session.toDTO();
    }

    @Override
    @Transactional
    public SessionDTO archiveSession(String sessionId) {
        Session session = sessionRepository.selectById(sessionId);
        if (session == null) {
            throw new EntityNotFoundException("会话不存在: " + sessionId);
        }

        session.archive();
        sessionRepository.updateById(session);

        return session.toDTO();
    }

    @Override
    @Transactional
    public SessionDTO unarchiveSession(String sessionId) {
        Session session = sessionRepository.selectById(sessionId);
        if (session == null) {
            throw new EntityNotFoundException("会话不存在: " + sessionId);
        }

        session.unarchive();
        sessionRepository.updateById(session);

        return session.toDTO();
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        // 先删除会话关联的所有消息
        messageService.deleteSessionMessages(sessionId);

        // 删除会话关联的上下文
        contextService.deleteContext(sessionId);

        // 最后删除会话本身
        sessionRepository.deleteById(sessionId);
    }

    @Override
    public List<SessionDTO> searchSessions(String userId, String keyword) {
        return sessionRepository.findByUserIdAndTitleContainingOrderByUpdatedAtDesc(userId, keyword)
                .stream()
                .map(Session::toDTO)
                .collect(Collectors.toList());
    }
}