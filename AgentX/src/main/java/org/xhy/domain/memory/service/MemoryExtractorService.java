package org.xhy.domain.memory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.xhy.domain.memory.model.CandidateMemory;
import org.xhy.domain.memory.model.MemoryType;
import org.xhy.domain.rag.model.ModelConfig;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.rag.service.UserModelConfigResolver;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** 记忆抽取服务（对话后从一轮对话提取可长期复用的要点） */
@Service
public class MemoryExtractorService {

    private static final Logger log = LoggerFactory.getLogger(MemoryExtractorService.class);

    private static final String EXTRACT_PROMPT = """
            你是一名对话记忆提取器。你的任务是从“本轮用户发言”中抽取对后续多轮交互有复用价值的要点。

            一、类型定义（仅限以下四类）
            - PROFILE：用户稳定的偏好/人格特质/固定格式要求（例如“以后都用中文回答”“回答尽量附带 bash 示例”）。
            - TASK：明确的中长期目标/持续性计划（例如“一周内完成 Agent 项目并补齐文档”）。
            - FACT：与用户身份或工作环境相关、在较长时间内稳定不变的事实（例如“主要语言是 Python/我在上海/公司名称是 X”）。
            - EPISODIC：未来 3–5 轮内明显有帮助的情节性信息（短期上下文，但对后续几轮确有价值）。

            二、严格的“不抽取”判定（若仅包含以下内容，应输出空结果 <memories/>）
            - 一次性操作/命令/工具调用/浏览或文件系统操作的请求或描述（例如：查看/列出/打开/运行/下载/上传、发起检索/RAG、执行脚本、编译/安装等）。
              示例：“调用子 agent 查看 user 目录下的文件”“运行脚本 X”“ls/cd/cat/npm/pip/install”。
            - 仅与当轮问题相关的临时细节、临时数据或结论，后续复用价值不明确。
            - 含有隐私信息（身份证号、银行卡、精确住址、电话号码等）或敏感凭据（密钥、token、密码等）。

            三、提取与打分规则（先判定是否值得记忆，再决定是否输出）
            - 仅在“明确有助于后续多轮”的情况下才抽取；否则不要输出记忆。
            - importance 评分范围 0.0–1.0，基于以下维度综合评估：
              1) 稳定性/时效性（越长期稳定越高）；
              2) 复用价值（越可能反复用到越高）；
              3) 明确性/可执行性（越清晰具体越高）；
              4) 风险惩罚（涉及隐私/一次性操作则为 0）。
            - 仅输出 importance ≥ 0.8 的记忆；EPISODIC 更严格，需 importance ≥ 0.9。
            - 去重与合并：相同语义合并为 1 条；最多输出 1–3 条最高价值要点。
            - 文本应简洁可复用，避免逐字复述用户原话；不要输出命令、文件路径或一次性请求。

            四、输出格式（仅输出 XML，不要任何解释或其他文本）
            - 根节点：<memories>
            - 每条记忆：<memory>
                <type>PROFILE|TASK|FACT|EPISODIC</type>
                <text>...</text>
                <importance>0.0~1.0</importance>
                <tags><tag>t1</tag><tag>t2</tag></tags>  （可省略）
                <data>{可选的 JSON 对象字符串，如 {\"source\":\"heuristic\"}}</data>（可省略）
            - 若无可提取内容，输出 <memories/>。

            五、示例（必须遵循）
            A. 输入：“调用子 agent 查看 user 目录下的文件”
               输出：<memories/>
            B. 输入：“以后都用简体中文回答，并尽量给出 bash 示例”
               输出：
               <memories>
                 <memory>
                   <type>PROFILE</type>
                   <text>用户偏好简体中文回答，并偏好附带 bash 示例</text>
                   <importance>0.9</importance>
                   <tags><tag>preference</tag></tags>
                 </memory>
               </memories>
            C. 输入：“这周要把 Agent 项目搭建完并写文档”
               输出：
               <memories>
                 <memory>
                   <type>TASK</type>
                   <text>用户本周目标：完成 Agent 项目搭建并补齐文档</text>
                   <importance>0.9</importance>
                   <tags><tag>goal</tag></tags>
                 </memory>
               </memories>
            D. 输入：“我主要用 Python，平时在上海办公”
               输出：
               <memories>
                 <memory>
                   <type>FACT</type>
                   <text>用户主要使用 Python，常驻上海办公</text>
                   <importance>0.85</importance>
                   <tags><tag>background</tag></tags>
                 </memory>
               </memories>
            """;

    private final UserModelConfigResolver userModelConfigResolver;
    private final MemoryDomainService memoryDomainService;
    private final ObjectMapper objectMapper;

    public MemoryExtractorService(UserModelConfigResolver userModelConfigResolver,
            MemoryDomainService memoryDomainService) {
        this.userModelConfigResolver = userModelConfigResolver;
        this.memoryDomainService = memoryDomainService;
        this.objectMapper = new ObjectMapper();
    }

    /** 异步抽取并持久化（供外部直接调用，无需处理返回值） */
    @Async("memoryTaskExecutor")
    public void extractAndPersistAsync(String userId, String sessionId, String userMessage) {
        try {
            List<CandidateMemory> candidates = extract(userId, sessionId, userMessage);
            if (candidates != null && !candidates.isEmpty()) {
                memoryDomainService.saveMemories(userId, sessionId, candidates);
            }
        } catch (Exception e) {
            log.warn("async extract&persist failed userId={}, sessionId={}, err={}", userId, sessionId, e.getMessage());
        }
    }

    /** 抽取候选记忆（仅基于用户当轮发言）
     * @param userId 用户ID
     * @param sessionId 会话ID（仅记录来源）
     * @param userMessage 用户消息
     * @return 候选记忆列表（可能为空） */
    public List<CandidateMemory> extract(String userId, String sessionId, String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return new ArrayList<>();
        }
        try {
            // 使用用户默认聊天模型
            ModelConfig chatCfg = userModelConfigResolver.getUserChatModelConfig(userId);
            ChatModel chatModel = LLMProviderService.getStrand(chatCfg.getProtocol(),
                    new org.xhy.infrastructure.llm.config.ProviderConfig(chatCfg.getApiKey(), chatCfg.getBaseUrl(),
                            chatCfg.getModelEndpoint(), chatCfg.getProtocol()));

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage(EXTRACT_PROMPT));
            if (StringUtils.hasText(userMessage)) {
                messages.add(new UserMessage(userMessage.trim()));
            }

            ChatResponse resp = chatModel.chat(messages);

            String xml = resp.aiMessage().text();
            if (!StringUtils.hasText(xml)) {
                return new ArrayList<>();
            }

            return parseXmlMemories(xml);
        } catch (Exception e) {
            log.warn("记忆抽取失败 userId={}, err={}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Double asDouble(Object o) {
        if (o instanceof Number n)
            return n.doubleValue();
        try {
            return o == null ? null : Double.parseDouble(String.valueOf(o));
        } catch (Exception ignore) {
            return null;
        }
    }

    private List<CandidateMemory> parseXmlMemories(String xml) {
        List<CandidateMemory> out = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            if (root == null || !"memories".equalsIgnoreCase(root.getNodeName())) {
                return out;
            }

            NodeList list = root.getElementsByTagName("memory");
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE)
                    continue;
                Element el = (Element) node;

                String type = childText(el, "type");
                String text = childText(el, "text");
                String importanceStr = childText(el, "importance");
                Double imp = asDouble(importanceStr);
                Float importance = imp == null ? 0.5f : imp.floatValue();

                List<String> tags = new ArrayList<>();
                NodeList tagsNodes = el.getElementsByTagName("tags");
                if (tagsNodes.getLength() > 0) {
                    NodeList tagNodes = ((Element) tagsNodes.item(0)).getElementsByTagName("tag");
                    for (int j = 0; j < tagNodes.getLength(); j++) {
                        Node tagNode = tagNodes.item(j);
                        if (tagNode.getNodeType() == Node.ELEMENT_NODE) {
                            String t = tagNode.getTextContent();
                            if (StringUtils.hasText(t))
                                tags.add(t.trim());
                        }
                    }
                }

                Map<String, Object> data = null;
                String dataStr = childText(el, "data");
                if (StringUtils.hasText(dataStr)) {
                    try {
                        data = objectMapper.readValue(dataStr, new TypeReference<Map<String, Object>>() {
                        });
                    } catch (Exception ignore) {
                        // 如果 data 不是 JSON，则忽略
                    }
                }

                if (!StringUtils.hasText(text))
                    continue;
                CandidateMemory cm = new CandidateMemory();
                cm.setType(MemoryType.safeOf(type));
                cm.setText(text.trim());
                cm.setImportance(importance);
                cm.setTags(tags);
                cm.setData(data);
                out.add(cm);
            }
        } catch (Exception ignore) {
            // ignore XML parse errors to allow JSON fallback
        }
        return out;
    }

    private static String childText(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0)
            return null;
        String v = nl.item(0).getTextContent();
        return v == null ? null : v.trim();
    }
}
