package org.xhy.domain.conversation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.repository.ContextRepository;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;

@Service
public class ContextDomainService {

    private final ContextRepository contextRepository;

    public ContextDomainService(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    // 获取历史消息id
    public ContextEntity getBySession(String sessionId) {
        LambdaQueryWrapper<ContextEntity> wrapper = Wrappers.<ContextEntity>lambdaQuery()
                .eq(ContextEntity::getSessionId, sessionId)
                .select();
        ContextEntity contextEntity = contextRepository.selectOne(wrapper);
        if (contextEntity==null){
            throw new BusinessException("消息上下文不存在");
        }
        return contextEntity;
    }
}
