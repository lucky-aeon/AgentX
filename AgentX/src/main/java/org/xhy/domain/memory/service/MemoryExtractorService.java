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
            你是一名对话记忆提取器。你的任务是从本轮对话中抽取有长期复用价值的记忆要点。

            仅抽取以下类型：
            - PROFILE：用户偏好/人格特质（例如语气、格式偏好、禁忌）
            - TASK：用户的中长期目标/任务
            - FACT：稳定事实（不会随时间变化或与身份绑定的信息）
            - EPISODIC：对后续几轮仍有帮助的情节性信息

            要求：
            - 忽略一次性细节、临时数据、隐私信息（如身份证、银行卡、精确住址等）。
            - 避免重复；将相同语义的内容合并。
            - 只输出 XML，不要解释或添加其他文本。
            - XML 根节点为 <memories>，每个记忆为 <memory>，包含子节点：
              <type>PROFILE|TASK|FACT|EPISODIC</type>
              <text>...</text>
              <importance>0.0~1.0</importance>
              <tags><tag>t1</tag><tag>t2</tag></tags>  （可省略或为空）
              <data>{可选的JSON对象字符串}</data>   （可省略或为空）
            - 若无可提取内容，输出 <memories/>。
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

    /**
     * 异步抽取并持久化（供外部直接调用，无需处理返回值）
     */
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
        if (o instanceof Number n) return n.doubleValue();
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
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;
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
                            if (StringUtils.hasText(t)) tags.add(t.trim());
                        }
                    }
                }

                Map<String, Object> data = null;
                String dataStr = childText(el, "data");
                if (StringUtils.hasText(dataStr)) {
                    try {
                        data = objectMapper.readValue(dataStr, new TypeReference<Map<String, Object>>() {});
                    } catch (Exception ignore) {
                        // 如果 data 不是 JSON，则忽略
                    }
                }

                if (!StringUtils.hasText(text)) continue;
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
        if (nl.getLength() == 0) return null;
        String v = nl.item(0).getTextContent();
        return v == null ? null : v.trim();
    }
}
