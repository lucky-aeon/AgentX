package org.xhy.domain.conversation.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.repository.ContextRepository;
import org.xhy.domain.conversation.repository.MessageRepository;

@Service
public class MessageDomainService {

    private final MessageRepository messageRepository;

    private final ContextRepository contextRepository;

    public MessageDomainService(MessageRepository messageRepository, ContextRepository contextRepository) {
        this.messageRepository = messageRepository;
        this.contextRepository = contextRepository;
    }

    public List<MessageEntity> listByIds(List<String> ids) {
        return messageRepository.selectByIds(ids);
    }

    /** 保存消息并且更新消息到上下文 */
    public void saveMessageAndUpdateContext(List<MessageEntity> messageEntities, ContextEntity contextEntity) {
        if (messageEntities == null || messageEntities.isEmpty()) {
            return;
        }
        for (MessageEntity messageEntity : messageEntities) {
            messageEntity.setId(null);
        }
        messageRepository.insert(messageEntities);
        contextEntity.getActiveMessages().addAll(messageEntities.stream().map(MessageEntity::getId).toList());
        contextRepository.insertOrUpdate(contextEntity);
    }

    /** 保存消息 */
    public void saveMessage(List<MessageEntity> messageEntities) {
        messageRepository.insert(messageEntities);
    }

    public void updateMessage(MessageEntity message) {
        messageRepository.updateById(message);
    }
}
