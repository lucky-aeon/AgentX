package org.xhy.domain.conversation.service;


import org.springframework.stereotype.Service;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.repository.MessageRepository;

import java.util.List;

@Service
public class MessageDomainService {


    private final MessageRepository messageRepository;

    public MessageDomainService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }


    public List<MessageEntity> listByIds(List<String> ids){
        return messageRepository.selectByIds(ids);
    }
}
