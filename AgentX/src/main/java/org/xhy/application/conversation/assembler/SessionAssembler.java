package org.xhy.application.conversation.assembler;

import org.xhy.application.conversation.dto.SessionDTO;
import org.xhy.domain.conversation.model.SessionEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SessionAssembler {

    public static SessionDTO toDTO(SessionEntity session) {
        SessionDTO sessionDTO = new SessionDTO();

        sessionDTO.setId(session.getId());
        sessionDTO.setTitle(session.getTitle());
        sessionDTO.setAgentId(session.getAgentId());
        sessionDTO.setCreatedAt(session.getCreatedAt());
        sessionDTO.setUpdatedAt(session.getUpdatedAt());
        sessionDTO.setDescription(session.getDescription());
        sessionDTO.setArchived(session.isArchived());
        return sessionDTO;
    }

    public static SessionEntity toEntity(SessionDTO sessionDTO) {
        SessionEntity session = new SessionEntity();
        session.setId(sessionDTO.getId());
        session.setTitle(sessionDTO.getTitle());
        session.setAgentId(sessionDTO.getAgentId());
        session.setCreatedAt(sessionDTO.getCreatedAt());
        session.setUpdatedAt(sessionDTO.getUpdatedAt());
        session.setDescription(sessionDTO.getDescription());
        session.setArchived(sessionDTO.isArchived());
        return session;
    }

    public static List<SessionDTO> toDTOs(List<SessionEntity> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyList();
        }
        return sessions.stream().map(SessionAssembler::toDTO).collect(Collectors.toList());
    }
}
