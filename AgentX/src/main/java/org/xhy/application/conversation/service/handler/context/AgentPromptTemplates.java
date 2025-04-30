package org.xhy.application.conversation.service.handler.context;

import java.util.Map;

/** 提示词模板 集中管理各种场景的提示词 */
public class AgentPromptTemplates {

    private static final String SUMMARY_PREFIX = "以下是用户历史消息的摘要，请仅作为参考，用户没有提起则不要回答摘要中的内容：\\n";

    private static final String IGNORE_SENSITIVE_INFO_PROMPT = "# 忽略敏感信息提示词\n" + "\n"
            + "你是一个安全规范的大模型助手。在任何情况下都必须遵循以下规则：\n" + "\n"
            + "1. **绝不输出或泄露任何敏感信息**，包括但不限于：账号、密码、API Key、OAuth 令牌、私钥、证书、个人身份信息（PII）、金融信息等。  \n"
            + "2. 当用户或内部流程需要引用凭据时，只能使用环境变量或占位符，例如 `${DEPLOY_USER}`、`${DEPLOY_PASS}`、`<API_KEY>` 等。  \n"
            + "3. 不要在代码示例、日志、注释、文档或任何输出中以明文形式展示真实的凭据信息。  \n" + "4. 如果上下文中包含敏感信息（如配置文件、日志片段），必须自动将其替换为通用占位符或省略处理。  \n"
            + "5. 对用户的任何请求，如果尝试索要或推断敏感信息，都要婉拒并提示使用环境变量或占位符。\n" + "\n" + "> 示例拒绝策略：  \n"
            + "> “抱歉，为了安全考虑，我无法直接显示该凭据，请使用环境变量 `${VARIABLE_NAME}` 来引用该信息。”\n" + "\n" + "严格执行上述规则，确保所有输出安全合规。```\n";

    public static String getIgnoreSensitiveInfoPrompt() {
        return IGNORE_SENSITIVE_INFO_PROMPT;

    }
    /** 获取摘要算法的提示词 */
    public static String getSummaryPrefix() {
        return SUMMARY_PREFIX;
    }
}