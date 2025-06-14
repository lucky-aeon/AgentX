package org.xhy.interfaces.api.external;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 外部API控制器 提供给外部系统的API接口，遵循OpenAI协议 使用API Key进行身份验证 */
@RestController
@RequestMapping("/v1")
public class LLMAPIController {

    // TODO: 后续实现对话、模型列表、会话列表、历史消息等API
    // 这些API将使用API Key进行身份验证，调用对应的应用服务

}
