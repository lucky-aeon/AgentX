package org.xhy.application.rag.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.knowledgeGraph.dto.GraphQueryResponse;
import org.xhy.application.knowledgeGraph.service.GraphQueryService;
import org.xhy.application.rag.dto.KgEnhancedRagRequest;
import org.xhy.application.rag.dto.EntityExtractionResponse;
import org.xhy.domain.neo4j.constant.EntityExtractionPrompt;
import org.xhy.infrastructure.json.Neo4jCompatibleJsonParser;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import com.alibaba.fastjson2.JSON;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * 图谱实体提取服务
 * 从查询文本中提取实体并在知识图谱中查找相关信息
 * 
 * @author AgentX
 */
@Service
public class GraphEntityExtractorService {

    private static final Logger log = LoggerFactory.getLogger(GraphEntityExtractorService.class);

    private final GraphQueryService graphQueryService;

    // 预定义的实体类型和关键词模式
    private static final Map<String, List<Pattern>> ENTITY_PATTERNS = new HashMap<>();
    private static final Set<String> STOP_WORDS = new HashSet<>();
    private static final Set<String> TECHNICAL_KEYWORDS = new HashSet<>();

    static {
        // 初始化停用词
        STOP_WORDS.addAll(Arrays.asList(
            "的", "是", "在", "有", "和", "与", "或", "但", "然而", "因为", "所以",
            "如何", "什么", "哪里", "为什么", "怎么", "能够", "可以", "应该", "需要",
            "关于", "对于", "通过", "根据", "按照", "依据", "基于", "由于", "为了",
            "这个", "那个", "这些", "那些", "它们", "我们", "你们", "他们", "她们"
        ));

        // 初始化技术关键词
        TECHNICAL_KEYWORDS.addAll(Arrays.asList(
            "算法", "模型", "系统", "平台", "架构", "框架", "技术", "方法", "策略",
            "服务", "接口", "API", "数据库", "缓存", "队列", "消息", "事件", "流程",
            "配置", "部署", "监控", "日志", "异常", "错误", "性能", "优化", "扩展"
        ));

        // 初始化实体识别模式
        ENTITY_PATTERNS.put("PERSON", Arrays.asList(
            Pattern.compile("[\\u4e00-\\u9fa5]{2,4}(?:先生|女士|老师|教授|博士|工程师)?"),
            Pattern.compile("\\b[A-Z][a-z]+\\s+[A-Z][a-z]+\\b")
        ));

        ENTITY_PATTERNS.put("ORGANIZATION", Arrays.asList(
            Pattern.compile("[\\u4e00-\\u9fa5]+(?:公司|集团|企业|机构|组织|部门|团队)"),
            Pattern.compile("\\b[A-Z][a-zA-Z]*\\s*(?:Inc|Corp|Ltd|Company|Organization)\\b")
        ));

        ENTITY_PATTERNS.put("TECHNOLOGY", Arrays.asList(
            Pattern.compile("(?:Java|Python|JavaScript|React|Spring|MySQL|Redis|Docker|Kubernetes)"),
            Pattern.compile("[A-Z][a-z]*(?:Service|Controller|Repository|Manager|Handler|Processor)")
        ));

        ENTITY_PATTERNS.put("CONCEPT", Arrays.asList(
            Pattern.compile("[\\u4e00-\\u9fa5]{2,8}(?:系统|平台|架构|框架|模式|策略|方案)")
        ));
    }

    public GraphEntityExtractorService(GraphQueryService graphQueryService) {
        this.graphQueryService = graphQueryService;
    }

    /**
     * 从查询中提取实体并查询图谱信息
     * 
     * @param question 查询问题
     * @param strategy 提取策略
     * @param maxDepth 最大图谱遍历深度
     * @param maxRelationsPerEntity 每个实体的最大关系数
     * @return 实体查询结果
     */
    public EntityExtractionResult extractEntitiesAndQuery(String question, 
                                                         KgEnhancedRagRequest.EntityExtractionStrategy strategy,
                                                         int maxDepth, 
                                                         int maxRelationsPerEntity) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("开始实体提取，策略: {}, 查询: '{}'", strategy, question);

            // 1. 提取实体
            Set<ExtractedEntity> extractedEntities = extractEntities(question, strategy);
            log.debug("提取到 {} 个实体: {}", extractedEntities.size(), 
                extractedEntities.stream().map(ExtractedEntity::getText).collect(Collectors.toList()));

            // 2. 在图谱中查询实体
            List<GraphQueryResponse.NodeResult> graphNodes = new ArrayList<>();
            List<GraphQueryResponse.RelationshipResult> graphRelationships = new ArrayList<>();
            int queryCount = 0;

            for (ExtractedEntity entity : extractedEntities) {
                try {
                    queryEntityInGraph(entity, graphNodes, graphRelationships, maxDepth, maxRelationsPerEntity);
                    queryCount++;
                } catch (Exception e) {
                    log.warn("查询实体 '{}' 失败: {}", entity.getText(), e.getMessage());
                }
            }

            // 3. 构建结果
            EntityExtractionResult result = new EntityExtractionResult();
            result.setExtractedEntities(new ArrayList<>(extractedEntities));
            result.setGraphNodes(graphNodes);
            result.setGraphRelationships(graphRelationships);
            result.setQueryCount(queryCount);
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            log.debug("实体提取完成，找到 {} 个图谱节点, {} 个关系，执行了 {} 次查询",
                graphNodes.size(), graphRelationships.size(), queryCount);

            return result;

        } catch (Exception e) {
            log.error("实体提取失败", e);
            EntityExtractionResult result = new EntityExtractionResult();
            result.setExtractedEntities(new ArrayList<>());
            result.setGraphNodes(new ArrayList<>());
            result.setGraphRelationships(new ArrayList<>());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * 从文本中提取实体
     */
    private Set<ExtractedEntity> extractEntities(String text, KgEnhancedRagRequest.EntityExtractionStrategy strategy) {
        switch (strategy) {
            case KEYWORD:
                return extractEntitiesByKeyword(text);
            case NER:
                return extractEntitiesByNER(text);
            case LLM:
                return extractEntitiesByLLM(text);
            default:
                return extractEntitiesByKeyword(text);
        }
    }

    /**
     * 基于关键词的实体提取
     */
    private Set<ExtractedEntity> extractEntitiesByKeyword(String text) {
        Set<ExtractedEntity> entities = new HashSet<>();
        
        // 1. 基于预定义模式提取
        for (Map.Entry<String, List<Pattern>> entry : ENTITY_PATTERNS.entrySet()) {
            String entityType = entry.getKey();
            for (Pattern pattern : entry.getValue()) {
                pattern.matcher(text).results().forEach(match -> {
                    String entityText = match.group().trim();
                    if (!isStopWord(entityText) && entityText.length() >= 2) {
                        entities.add(new ExtractedEntity(entityText, entityType, match.start(), match.end()));
                    }
                });
            }
        }

        // 2. 基于词性和长度的通用提取
        String[] words = text.replaceAll("[，。！？；：、'（）【】\\s]+", " ").split("\\s+");
        for (String word : words) {
            word = word.trim();
            if (isLikelyEntity(word)) {
                entities.add(new ExtractedEntity(word, "UNKNOWN", 0, 0));
            }
        }

        return entities;
    }

    /**
     * 基于NER的实体提取（简化版本，实际应该集成专业NER工具）
     */
    private Set<ExtractedEntity> extractEntitiesByNER(String text) {
        // 这里应该集成如Stanford NLP、HanLP等NER工具
        // 目前使用关键词提取作为fallback
        log.warn("NER实体提取尚未实现，使用关键词提取");
        return extractEntitiesByKeyword(text);
    }

    /**
     * 基于LLM的实体提取
     * 使用大语言模型从文本中智能提取实体信息
     * 支持多种实体类型识别和置信度评估
     */
    private Set<ExtractedEntity> extractEntitiesByLLM(String text) {
        try {
            log.debug("开始使用LLM进行实体提取，文本长度: {}", text.length());
            
            // 1. 替换prompt模板中的占位符
            final String formattedPrompt = EntityExtractionPrompt.entityExtractionPrompt.replace("{{text}}", text);
            final UserMessage userMessage = UserMessage.userMessage(formattedPrompt);
            
            log.debug("准备发送给AI的prompt长度: {}", formattedPrompt.length());

            // 2. 创建实体提取的模型配置 - 使用固定配置（后续可改为从用户配置获取）
            ProviderConfig entityExtractionProviderConfig = new ProviderConfig(
                    "sk-b9aL4HqXa1OHY6TyctHBVnjq9IQndYd5snq4WdX9sQ4DUFma", 
                    "https://new.281182.xyz/v1",
                    "gemini-2.5-pro", 
                    ProviderProtocol.OPENAI);

            ChatModel entityExtractionModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, entityExtractionProviderConfig);

            // 3. 调用LLM进行实体提取
            final ChatResponse chat = entityExtractionModel.chat(userMessage);
            final String responseText = chat.aiMessage().text();
            
            log.debug("LLM返回的原始文本: {}", responseText);

            // 4. 解析LLM返回的JSON结果
            Set<ExtractedEntity> entities = parseEntityExtractionResponse(responseText);
            
            log.info("LLM实体提取完成，提取到 {} 个实体", entities.size());
            return entities;
            
        } catch (Exception e) {
            log.error("LLM实体提取失败，使用关键词提取作为fallback: {}", e.getMessage(), e);
            return extractEntitiesByKeyword(text);
        }
    }
    
    /**
     * 解析LLM返回的实体提取结果
     */
    private Set<ExtractedEntity> parseEntityExtractionResponse(String responseText) {
        Set<ExtractedEntity> entities = new HashSet<>();
        
        try {
            // 1. 清理响应文本，提取JSON部分
            String cleanedJson = extractJsonFromText(responseText);
            
            // 2. 解析JSON
            EntityExtractionResponse response = Neo4jCompatibleJsonParser.parseObject(cleanedJson, EntityExtractionResponse.class);
            
            if (response != null && response.getEntities() != null) {
                // 3. 转换为ExtractedEntity对象
                for (EntityExtractionResponse.EntityItem item : response.getEntities()) {
                    if (item.getText() != null && !item.getText().trim().isEmpty()) {
                        ExtractedEntity entity = new ExtractedEntity(
                            item.getText().trim(), 
                            item.getType() != null ? item.getType() : "UNKNOWN", 
                            0, 0
                        );
                        entity.setConfidence(item.getConfidence());
                        entities.add(entity);
                    }
                }
            }
            
            log.debug("成功解析LLM返回结果，提取到 {} 个实体", entities.size());
            
        } catch (Exception e) {
            log.error("解析LLM实体提取结果失败: {}", e.getMessage(), e);
            log.debug("原始响应文本: {}", responseText);
        }
        
        return entities;
    }
    
    /**
     * 从AI返回的文本中提取JSON内容
     * 处理可能包含Markdown代码块或其他格式的文本
     */
    private String extractJsonFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "{}";
        }
        
        // 去除首尾空白
        text = text.trim();
        
        // 如果文本以```json或```开头，提取代码块中的内容
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                int lastTripleBacktick = text.lastIndexOf("```");
                if (lastTripleBacktick > firstNewline) {
                    text = text.substring(firstNewline + 1, lastTripleBacktick).trim();
                }
            }
        }
        
        // 查找第一个{和最后一个}，提取JSON部分
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1);
        }
        
        // 如果还是不是有效的JSON格式，返回空对象
        if (!text.startsWith("{") || !text.endsWith("}")) {
            log.warn("无法从文本中提取有效的JSON格式，返回空对象");
            return "{}";
        }
        
        return text;
    }

    /**
     * 判断词汇是否可能是实体
     */
    private boolean isLikelyEntity(String word) {
        if (word.length() < 2 || word.length() > 20) {
            return false;
        }
        
        if (isStopWord(word)) {
            return false;
        }
        
        // 技术关键词优先
        if (TECHNICAL_KEYWORDS.contains(word)) {
            return true;
        }
        
        // 中文词汇（2-8个字符）
        if (word.matches("[\\u4e00-\\u9fa5]{2,8}")) {
            return true;
        }
        
        // 英文词汇（首字母大写，2-15个字符）
        if (word.matches("[A-Z][a-zA-Z]{1,14}")) {
            return true;
        }
        
        return false;
    }

    /**
     * 检查是否为停用词
     */
    private boolean isStopWord(String word) {
        return STOP_WORDS.contains(word.toLowerCase());
    }

    /**
     * 在知识图谱中查询实体
     */
    private void queryEntityInGraph(ExtractedEntity entity, 
                                   List<GraphQueryResponse.NodeResult> graphNodes,
                                   List<GraphQueryResponse.RelationshipResult> graphRelationships,
                                   int maxDepth, 
                                   int maxRelationsPerEntity) {
        try {
            // 1. 精确匹配查询
            GraphQueryResponse exactMatch = graphQueryService.findNodesByProperty(
                "GenericNode", "name", entity.getText(), 5);
            
            if (exactMatch.isSuccess() && exactMatch.getNodes() != null && !exactMatch.getNodes().isEmpty()) {
                processGraphQueryResult(exactMatch, graphNodes, graphRelationships, maxRelationsPerEntity);
                return;
            }

            // 2. 模糊匹配查询（包含查询文本的节点）
            List<GraphQueryResponse.NodeResult> fuzzyMatches = findNodesByFuzzyMatch(entity.getText());
            if (!fuzzyMatches.isEmpty()) {
                graphNodes.addAll(fuzzyMatches);
                
                // 为模糊匹配的节点查询关系
                for (GraphQueryResponse.NodeResult node : fuzzyMatches) {
                    GraphQueryResponse relationResults = graphQueryService.findNodeRelationships(
                        node.getId(), null, "BOTH", maxRelationsPerEntity);
                    
                    if (relationResults.isSuccess()) {
                        if (relationResults.getRelationships() != null) {
                            graphRelationships.addAll(relationResults.getRelationships());
                        }
                        if (relationResults.getNodes() != null) {
                            graphNodes.addAll(relationResults.getNodes());
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.debug("查询实体 '{}' 时发生错误: {}", entity.getText(), e.getMessage());
        }
    }

    /**
     * 处理图谱查询结果
     */
    private void processGraphQueryResult(GraphQueryResponse queryResult,
                                       List<GraphQueryResponse.NodeResult> graphNodes,
                                       List<GraphQueryResponse.RelationshipResult> graphRelationships,
                                       int maxRelationsPerEntity) {
        if (queryResult.getNodes() != null) {
            graphNodes.addAll(queryResult.getNodes());
            
            // 为每个找到的节点查询其关系
            for (GraphQueryResponse.NodeResult node : queryResult.getNodes()) {
                try {
                    GraphQueryResponse relationResults = graphQueryService.findNodeRelationships(
                        node.getId(), null, "BOTH", maxRelationsPerEntity);
                    
                    if (relationResults.isSuccess()) {
                        if (relationResults.getRelationships() != null) {
                            graphRelationships.addAll(relationResults.getRelationships());
                        }
                        if (relationResults.getNodes() != null) {
                            // 避免重复添加节点
                            List<GraphQueryResponse.NodeResult> newNodes = relationResults.getNodes().stream()
                                .filter(newNode -> graphNodes.stream()
                                    .noneMatch(existingNode -> existingNode.getId().equals(newNode.getId())))
                                .collect(Collectors.toList());
                            graphNodes.addAll(newNodes);
                        }
                    }
                } catch (Exception e) {
                    log.debug("查询节点 '{}' 的关系时发生错误: {}", node.getId(), e.getMessage());
                }
            }
        }
        
        if (queryResult.getRelationships() != null) {
            graphRelationships.addAll(queryResult.getRelationships());
        }
    }

    /**
     * 模糊匹配查找节点（简化版本）
     */
    private List<GraphQueryResponse.NodeResult> findNodesByFuzzyMatch(String entityText) {
        // 这里应该实现更复杂的模糊匹配逻辑
        // 目前返回空列表，实际应该查询包含关键词的节点
        return new ArrayList<>();
    }

    /**
     * 提取的实体信息
     */
    public static class ExtractedEntity {
        private String text;
        private String type;
        private int startPosition;
        private int endPosition;
        private double confidence = 1.0;

        public ExtractedEntity(String text, String type, int startPosition, int endPosition) {
            this.text = text;
            this.type = type;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }

        // Getters and Setters
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getStartPosition() {
            return startPosition;
        }

        public void setStartPosition(int startPosition) {
            this.startPosition = startPosition;
        }

        public int getEndPosition() {
            return endPosition;
        }

        public void setEndPosition(int endPosition) {
            this.endPosition = endPosition;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExtractedEntity that = (ExtractedEntity) o;
            return Objects.equals(text, that.text) && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, type);
        }

        @Override
        public String toString() {
            return "ExtractedEntity{" +
                    "text='" + text + '\'' +
                    ", type='" + type + '\'' +
                    ", confidence=" + confidence +
                    '}';
        }
    }

    /**
     * 实体提取结果
     */
    public static class EntityExtractionResult {
        private List<ExtractedEntity> extractedEntities;
        private List<GraphQueryResponse.NodeResult> graphNodes;
        private List<GraphQueryResponse.RelationshipResult> graphRelationships;
        private int queryCount;
        private long processingTimeMs;

        // Getters and Setters
        public List<ExtractedEntity> getExtractedEntities() {
            return extractedEntities;
        }

        public void setExtractedEntities(List<ExtractedEntity> extractedEntities) {
            this.extractedEntities = extractedEntities;
        }

        public List<GraphQueryResponse.NodeResult> getGraphNodes() {
            return graphNodes;
        }

        public void setGraphNodes(List<GraphQueryResponse.NodeResult> graphNodes) {
            this.graphNodes = graphNodes;
        }

        public List<GraphQueryResponse.RelationshipResult> getGraphRelationships() {
            return graphRelationships;
        }

        public void setGraphRelationships(List<GraphQueryResponse.RelationshipResult> graphRelationships) {
            this.graphRelationships = graphRelationships;
        }

        public int getQueryCount() {
            return queryCount;
        }

        public void setQueryCount(int queryCount) {
            this.queryCount = queryCount;
        }

        public long getProcessingTimeMs() {
            return processingTimeMs;
        }

        public void setProcessingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
        }
    }
}
