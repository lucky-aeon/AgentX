package org.xhy.application.knowledgeGraph.service;

import com.alibaba.fastjson.JSON;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest.EntityDto;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest.RelationshipDto;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionResponse;
import org.xhy.domain.neo4j.constant.EnhancedGraphExtractorPrompt;
import org.xhy.infrastructure.json.Neo4jCompatibleJsonParser;
import org.xhy.infrastructure.llm.LLMProviderService;
import org.xhy.infrastructure.llm.config.ProviderConfig;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档结构化服务
 * 负责在所有页面处理完成后，创建文档级的主节点和层次结构
 * 
 * @author AgentX
 */
@Service
public class DocumentStructuringService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentStructuringService.class);

    private final GraphIngestionAppService graphIngestionAppService;

    public DocumentStructuringService(GraphIngestionAppService graphIngestionAppService) {
        this.graphIngestionAppService = graphIngestionAppService;
    }

    /**
     * 为文档创建结构化的知识图谱
     * 包括主节点、主题节点和层次关系
     * 
     * @param documentId 文档ID
     * @param allEntities 所有页面的实体
     * @param allRelationships 所有页面的关系
     * @param totalPages 总页数
     * @return 结构化结果
     */
    @Transactional("neo4jTransactionManager")
    public GraphIngestionResponse createDocumentStructure(
            String documentId, 
            List<EntityDto> allEntities, 
            List<RelationshipDto> allRelationships,
            Integer totalPages) {

        logger.info("开始为文档 {} 创建结构化知识图谱，实体数: {}, 关系数: {}, 页数: {}",
                documentId, allEntities.size(), allRelationships.size(), totalPages);

        try {
            // 1. 分析实体重要性和主题分布
            DocumentStructureAnalysis analysis = analyzeDocumentStructure(allEntities, allRelationships, totalPages);

            // 2. 使用AI生成文档结构
            DocumentStructureResult structureResult = generateDocumentStructureWithAI(
                    documentId, allEntities, allRelationships, analysis, totalPages);

            // 3. 创建最终的图谱请求
            GraphIngestionRequest finalRequest = buildFinalGraphRequest(
                    documentId, structureResult, allEntities, allRelationships);

            // 4. 保存到Neo4j
            GraphIngestionResponse response = graphIngestionAppService.ingestGraphData(finalRequest);

            logger.info("文档 {} 结构化完成，新增主节点: {}, 主题节点: {}, 层次关系: {}",
                    documentId, 1, structureResult.topicNodes.size(), structureResult.hierarchicalRelationships.size());

            return response;

        } catch (Exception e) {
            logger.error("文档 {} 结构化失败: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("文档结构化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分析文档结构
     */
    private DocumentStructureAnalysis analyzeDocumentStructure(
            List<EntityDto> entities, List<RelationshipDto> relationships, Integer totalPages) {

        DocumentStructureAnalysis analysis = new DocumentStructureAnalysis();

        // 分析实体重要性分布
        Map<String, Long> entityTypeDistribution = entities.stream()
                .flatMap(entity -> entity.getLabels().stream())
                .collect(Collectors.groupingBy(label -> label, Collectors.counting()));

        // 分析页面分布
        Map<Integer, Long> pageDistribution = entities.stream()
                .filter(entity -> entity.getPageNumber() != null)
                .collect(Collectors.groupingBy(EntityDto::getPageNumber, Collectors.counting()));

        // 识别高频实体（可能的主题）
        Map<String, Long> entityFrequency = entities.stream()
                .collect(Collectors.groupingBy(entity -> 
                        entity.getProperties().get("name") != null ? 
                                entity.getProperties().get("name").toString() : entity.getId(), 
                        Collectors.counting()));

        // 分析关系强度
        Map<String, Long> relationshipTypeDistribution = relationships.stream()
                .collect(Collectors.groupingBy(RelationshipDto::getType, Collectors.counting()));

        analysis.entityTypeDistribution = entityTypeDistribution;
        analysis.pageDistribution = pageDistribution;
        analysis.entityFrequency = entityFrequency;
        analysis.relationshipTypeDistribution = relationshipTypeDistribution;
        analysis.totalPages = totalPages;

        logger.debug("文档结构分析完成，实体类型: {}, 高频实体: {}", 
                entityTypeDistribution.size(), entityFrequency.size());

        return analysis;
    }

    /**
     * 使用AI生成文档结构
     */
    private DocumentStructureResult generateDocumentStructureWithAI(
            String documentId, List<EntityDto> entities, List<RelationshipDto> relationships,
            DocumentStructureAnalysis analysis, Integer totalPages) {

        try {
            // 构建结构化提示词
            String prompt = buildStructuringPrompt(documentId, entities, relationships, analysis, totalPages);

            // 调用AI
            String aiResponse = callAIForStructuring(prompt);

            // 解析AI响应
            return parseStructuringResponse(aiResponse);

        } catch (Exception e) {
            logger.warn("AI文档结构化失败，使用默认结构: {}", e.getMessage());
            return createDefaultDocumentStructure(documentId, entities, analysis, totalPages);
        }
    }

    /**
     * 构建结构化提示词
     */
    private String buildStructuringPrompt(String documentId, List<EntityDto> entities, 
                                        List<RelationshipDto> relationships,
                                        DocumentStructureAnalysis analysis, Integer totalPages) {

        String prompt = EnhancedGraphExtractorPrompt.documentStructuringPrompt;

        // 替换模板变量
        prompt = prompt.replace("{{documentId}}", documentId);
        prompt = prompt.replace("{{totalPages}}", String.valueOf(totalPages));
        prompt = prompt.replace("{{documentType}}", determineDocumentType(analysis));
        prompt = prompt.replace("{{totalEntities}}", String.valueOf(entities.size()));
        prompt = prompt.replace("{{totalRelationships}}", String.valueOf(relationships.size()));

        // 添加实体数据摘要
        String entitySummary = createEntitySummary(entities, analysis);
        prompt = prompt.replace("{{entityData}}", entitySummary);

        // 添加关系数据摘要
        String relationshipSummary = createRelationshipSummary(relationships, analysis);
        prompt = prompt.replace("{{relationshipData}}", relationshipSummary);

        return prompt;
    }

    /**
     * 确定文档类型
     */
    private String determineDocumentType(DocumentStructureAnalysis analysis) {
        // 基于实体类型分布判断文档类型
        Map<String, Long> typeDistribution = analysis.entityTypeDistribution;
        
        if (typeDistribution.containsKey("技术") || typeDistribution.containsKey("TECHNOLOGY")) {
            return "TechnicalDocument";
        } else if (typeDistribution.containsKey("人物") || typeDistribution.containsKey("PERSON")) {
            return "BiographicalDocument";
        } else if (typeDistribution.containsKey("组织") || typeDistribution.containsKey("ORGANIZATION")) {
            return "BusinessDocument";
        } else {
            return "GeneralDocument";
        }
    }

    /**
     * 创建实体数据摘要
     */
    private String createEntitySummary(List<EntityDto> entities, DocumentStructureAnalysis analysis) {
        StringBuilder summary = new StringBuilder();
        
        // 高重要性实体
        List<EntityDto> importantEntities = entities.stream()
                .filter(entity -> {
                    Object importance = entity.getProperties().get("importance");
                    return importance != null && Double.parseDouble(importance.toString()) > 0.7;
                })
                .limit(10)
                .collect(Collectors.toList());

        summary.append("重要实体:\n");
        for (EntityDto entity : importantEntities) {
            summary.append("- ").append(entity.getId())
                    .append(" (").append(String.join(",", entity.getLabels())).append(")")
                    .append(": ").append(entity.getProperties().get("name"))
                    .append("\n");
        }

        return summary.toString();
    }

    /**
     * 创建关系数据摘要
     */
    private String createRelationshipSummary(List<RelationshipDto> relationships, DocumentStructureAnalysis analysis) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("关系类型分布:\n");
        analysis.relationshipTypeDistribution.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> summary.append("- ").append(entry.getKey())
                        .append(": ").append(entry.getValue()).append("\n"));

        return summary.toString();
    }

    /**
     * 调用AI进行结构化
     */
    private String callAIForStructuring(String prompt) {
        try {
            ProviderConfig providerConfig = new ProviderConfig(
                    "sk-b9aL4HqXa1OHY6TyctHBVnjq9IQndYd5snq4WdX9sQ4DUFma",
                    "https://new.281182.xyz/v1",
                    "gemini-2.5-pro",
                    ProviderProtocol.OPENAI);

            ChatModel chatModel = LLMProviderService.getStrand(ProviderProtocol.OPENAI, providerConfig);

            final UserMessage userMessage = UserMessage.userMessage(TextContent.from(prompt));
            final ChatResponse chat = chatModel.chat(userMessage);

            return chat.aiMessage().text();

        } catch (Exception e) {
            logger.error("AI结构化调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析AI结构化响应
     */
    private DocumentStructureResult parseStructuringResponse(String aiResponse) {
        try {
            // 提取JSON并解析
            String cleanedJson = extractJsonFromText(aiResponse);
            Map<String, Object> responseMap = (Map<String, Object>) Neo4jCompatibleJsonParser.parseObject(cleanedJson);

            DocumentStructureResult result = new DocumentStructureResult();

            // 解析文档节点
            if (responseMap.containsKey("documentNode")) {
                result.documentNode = parseEntityFromMap((Map<String, Object>) responseMap.get("documentNode"));
            }

            // 解析主题节点
            if (responseMap.containsKey("topicNodes")) {
                List<Map<String, Object>> topicMaps = (List<Map<String, Object>>) responseMap.get("topicNodes");
                result.topicNodes = topicMaps.stream()
                        .map(this::parseEntityFromMap)
                        .collect(Collectors.toList());
            }

            // 解析层次关系
            if (responseMap.containsKey("hierarchicalRelationships")) {
                List<Map<String, Object>> relationshipMaps = (List<Map<String, Object>>) responseMap.get("hierarchicalRelationships");
                result.hierarchicalRelationships = relationshipMaps.stream()
                        .map(this::parseRelationshipFromMap)
                        .collect(Collectors.toList());
            }

            return result;

        } catch (Exception e) {
            logger.error("解析AI结构化响应失败: {}", e.getMessage(), e);
            throw new RuntimeException("响应解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从Map解析实体
     */
    private EntityDto parseEntityFromMap(Map<String, Object> entityMap) {
        EntityDto entity = new EntityDto();
        entity.setId((String) entityMap.get("id"));
        entity.setLabels((List<String>) entityMap.get("labels"));
        entity.setProperties((Map<String, Object>) entityMap.get("properties"));
        return entity;
    }

    /**
     * 从Map解析关系
     */
    private RelationshipDto parseRelationshipFromMap(Map<String, Object> relationshipMap) {
        RelationshipDto relationship = new RelationshipDto();
        relationship.setSourceId((String) relationshipMap.get("sourceId"));
        relationship.setTargetId((String) relationshipMap.get("targetId"));
        relationship.setType((String) relationshipMap.get("type"));
        relationship.setProperties((Map<String, Object>) relationshipMap.get("properties"));
        return relationship;
    }

    /**
     * 创建默认文档结构（AI失败时的备选方案）
     */
    private DocumentStructureResult createDefaultDocumentStructure(
            String documentId, List<EntityDto> entities, 
            DocumentStructureAnalysis analysis, Integer totalPages) {

        DocumentStructureResult result = new DocumentStructureResult();

        // 创建文档节点
        EntityDto documentNode = new EntityDto();
        documentNode.setId("DOC_" + documentId);
        documentNode.setLabels(Arrays.asList("Document", "GeneralDocument"));
        
        Map<String, Object> docProperties = new HashMap<>();
        docProperties.put("name", "Document_" + documentId);
        docProperties.put("description", "自动生成的文档节点");
        docProperties.put("totalPages", totalPages);
        docProperties.put("totalEntities", entities.size());
        documentNode.setProperties(docProperties);
        
        result.documentNode = documentNode;

        // 基于实体频率创建主题节点
        result.topicNodes = createDefaultTopicNodes(documentId, analysis);

        // 创建基本的层次关系
        result.hierarchicalRelationships = createDefaultHierarchicalRelationships(
                documentNode, result.topicNodes, entities);

        return result;
    }

    /**
     * 创建默认主题节点
     */
    private List<EntityDto> createDefaultTopicNodes(String documentId, DocumentStructureAnalysis analysis) {
        List<EntityDto> topicNodes = new ArrayList<>();

        // 基于实体类型创建主题
        analysis.entityTypeDistribution.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3) // 最多3个主题
                .forEach(entry -> {
                    EntityDto topicNode = new EntityDto();
                    topicNode.setId("TOPIC_" + documentId + "_" + entry.getKey());
                    topicNode.setLabels(Arrays.asList("Topic"));
                    
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("name", entry.getKey() + "主题");
                    properties.put("description", "包含" + entry.getValue() + "个" + entry.getKey() + "实体");
                    properties.put("importance", 0.8);
                    properties.put("entityCount", entry.getValue());
                    topicNode.setProperties(properties);
                    
                    topicNodes.add(topicNode);
                });

        return topicNodes;
    }

    /**
     * 创建默认层次关系
     */
    private List<RelationshipDto> createDefaultHierarchicalRelationships(
            EntityDto documentNode, List<EntityDto> topicNodes, List<EntityDto> entities) {
        
        List<RelationshipDto> relationships = new ArrayList<>();

        // 文档到主题的关系
        for (EntityDto topicNode : topicNodes) {
            RelationshipDto docToTopic = new RelationshipDto();
            docToTopic.setSourceId(documentNode.getId());
            docToTopic.setTargetId(topicNode.getId());
            docToTopic.setType("CONTAINS_TOPIC");
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("description", "文档包含主题");
            properties.put("hierarchyLevel", 1);
            docToTopic.setProperties(properties);
            
            relationships.add(docToTopic);
        }

        // 主题到实体的关系（基于实体标签匹配）
        for (EntityDto topicNode : topicNodes) {
            String topicType = topicNode.getId().split("_")[2]; // 提取主题类型
            
            entities.stream()
                    .filter(entity -> entity.getLabels().contains(topicType))
                    .limit(10) // 每个主题最多连接10个实体
                    .forEach(entity -> {
                        RelationshipDto topicToEntity = new RelationshipDto();
                        topicToEntity.setSourceId(topicNode.getId());
                        topicToEntity.setTargetId(entity.getId());
                        topicToEntity.setType("INCLUDES_ENTITY");
                        
                        Map<String, Object> properties = new HashMap<>();
                        properties.put("description", "主题包含实体");
                        properties.put("hierarchyLevel", 2);
                        topicToEntity.setProperties(properties);
                        
                        relationships.add(topicToEntity);
                    });
        }

        return relationships;
    }

    /**
     * 构建最终的图谱请求
     */
    private GraphIngestionRequest buildFinalGraphRequest(
            String documentId, DocumentStructureResult structureResult,
            List<EntityDto> originalEntities, List<RelationshipDto> originalRelationships) {

        List<EntityDto> finalEntities = new ArrayList<>();
        List<RelationshipDto> finalRelationships = new ArrayList<>();

        // 添加结构化节点
        if (structureResult.documentNode != null) {
            finalEntities.add(structureResult.documentNode);
        }
        finalEntities.addAll(structureResult.topicNodes);

        // 添加原始实体
        finalEntities.addAll(originalEntities);

        // 添加层次关系
        finalRelationships.addAll(structureResult.hierarchicalRelationships);

        // 添加原始关系
        finalRelationships.addAll(originalRelationships);

        return new GraphIngestionRequest(documentId, finalEntities, finalRelationships);
    }

    /**
     * JSON提取工具方法
     */
    private String extractJsonFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "{}";
        }

        text = text.trim();

        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                int lastTripleBacktick = text.lastIndexOf("```");
                if (lastTripleBacktick > firstNewline) {
                    text = text.substring(firstNewline + 1, lastTripleBacktick).trim();
                }
            }
        }

        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1);
        }

        return text;
    }

    /**
     * 文档结构分析结果
     */
    private static class DocumentStructureAnalysis {
        Map<String, Long> entityTypeDistribution = new HashMap<>();
        Map<Integer, Long> pageDistribution = new HashMap<>();
        Map<String, Long> entityFrequency = new HashMap<>();
        Map<String, Long> relationshipTypeDistribution = new HashMap<>();
        Integer totalPages;
    }

    /**
     * 文档结构化结果
     */
    private static class DocumentStructureResult {
        EntityDto documentNode;
        List<EntityDto> topicNodes = new ArrayList<>();
        List<RelationshipDto> hierarchicalRelationships = new ArrayList<>();
    }
}
