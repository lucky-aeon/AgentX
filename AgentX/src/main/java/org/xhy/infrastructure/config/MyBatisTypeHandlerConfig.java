package org.xhy.infrastructure.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.agent.model.LLMModelConfig;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.llm.model.config.ProviderConfig;
import org.xhy.domain.llm.model.enums.ModelType;
import org.xhy.domain.scheduledtask.constant.RepeatType;
import org.xhy.domain.scheduledtask.constant.ScheduleTaskStatus;
import org.xhy.domain.scheduledtask.model.RepeatConfig;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.constant.ToolType;
import org.xhy.domain.tool.constant.UploadType;
import org.xhy.infrastructure.converter.*;

import jakarta.annotation.PostConstruct;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.util.List;

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
        typeHandlerRegistry.register(List.class, new ListStringConverter());
        typeHandlerRegistry.register(LLMModelConfig.class, new LLMModelConfigConverter());
        typeHandlerRegistry.register(ProviderProtocol.class, new ProviderProtocolConverter());
        typeHandlerRegistry.register(ModelType.class, new ModelTypeConverter());
        typeHandlerRegistry.register(Role.class, new RoleConverter());
        typeHandlerRegistry.register(MessageType.class, new MessageTypeConverter());
        typeHandlerRegistry.register(ToolStatus.class, new ToolStatusConverter());
        typeHandlerRegistry.register(ToolType.class, new ToolTypeConverter());
        typeHandlerRegistry.register(UploadType.class, new UploadTypeConverter());
        
        // 定时任务相关的TypeHandler
        typeHandlerRegistry.register(RepeatType.class, new RepeatTypeConverter());
        typeHandlerRegistry.register(RepeatConfig.class, new RepeatConfigConverter());
        typeHandlerRegistry.register(ScheduleTaskStatus.class, new ScheduledTaskStatusConverter());

        log.info("手动注册类型处理器：ProviderConfigConverter");

        // 打印所有已注册的类型处理器
        log.info("已注册的类型处理器: {}", typeHandlerRegistry.getTypeHandlers().size());
    }
}