package org.xhy.domain.conversation.service;

import org.xhy.domain.conversation.model.Session;
import org.xhy.domain.conversation.model.SessionDTO;

import java.util.List;

/**
 * 会话服务接口
 */
public interface SessionService {

    /**
     * 创建新会话
     */
    SessionDTO createSession(String title, String userId, String description);

    /**
     * 获取单个会话
     */
    SessionDTO getSession(String sessionId);

    /**
     * 获取用户的所有会话
     */
    List<SessionDTO> getUserSessions(String userId);

    /**
     * 获取用户的活跃(非归档)会话
     */
    List<SessionDTO> getUserActiveSessions(String userId);

    /**
     * 获取用户的归档会话
     */
    List<SessionDTO> getUserArchivedSessions(String userId);

    /**
     * 更新会话信息
     */
    SessionDTO updateSession(String sessionId, String title, String description);

    /**
     * 归档会话
     */
    SessionDTO archiveSession(String sessionId);

    /**
     * 恢复归档的会话
     */
    SessionDTO unarchiveSession(String sessionId);

    /**
     * 删除会话
     */
    void deleteSession(String sessionId);

    /**
     * 搜索会话
     */
    List<SessionDTO> searchSessions(String userId, String keyword);
}