package org.xhy.infrastructure.config;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.xhy.domain.agent.model.LLMModelConfig;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.llm.model.config.ProviderConfig;
import org.xhy.domain.llm.model.enums.ModelType;
import org.xhy.domain.task.constant.TaskStatus;
import org.xhy.infrastructure.converter.*;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

/** MyBatis类型处理器配置类 用于手动注册类型处理器 */
@Configuration
public class MyBatisTypeHandlerConfig {

    private static final Logger log = LoggerFactory.getLogger(MyBatisTypeHandlerConfig.class);

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    /** 初始化注册类型处理器 */
    @PostConstruct
    public void registerTypeHandlers() {
        TypeHandlerRegistry typeHandlerRegistry = sqlSessionFactory.getConfiguration().getTypeHandlerRegistry();

        // 确保自动扫描没有生效时，我们手动注册需要的转换器
        typeHandlerRegistry.register(ProviderConfig.class, new ProviderConfigConverter());
        typeHandlerRegistry.register(List.class, new ListConverter());
        typeHandlerRegistry.register(LLMModelConfig.class, new LLMModelConfigConverter());
        typeHandlerRegistry.register(ProviderProtocol.class, new ProviderProtocolConverter());
        typeHandlerRegistry.register(ModelType.class, new ModelTypeConverter());
        typeHandlerRegistry.register(Role.class, new RoleConverter());
        typeHandlerRegistry.register(MessageType.class, new MessageTypeConverter());
        typeHandlerRegistry.register(TaskStatus.class, new TaskStatusConverter());

        log.info("手动注册类型处理器：ProviderConfigConverter");

        // 打印所有已注册的类型处理器
        log.info("已注册的类型处理器: {}", typeHandlerRegistry.getTypeHandlers().size());
    }
}
