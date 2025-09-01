package org.xhy.application.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.knowledgeGraph.dto.GraphQueryResponse;
import org.xhy.application.rag.assembler.DocumentUnitAssembler;
import org.xhy.application.rag.dto.DocumentUnitDTO;
import org.xhy.application.rag.dto.KgEnhancedRagRequest;
import org.xhy.application.rag.dto.KgEnhancedRagResponse;
import org.xhy.domain.rag.model.DocumentUnitEntity;

import java.util.*;
import java.util.stream.Collectors;

/** 混合检索策略服务 实现向量搜索和知识图谱查询的融合策略
 * 
 * @author AgentX */
@Service
public class HybridSearchStrategy {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchStrategy.class);

    /** 融合策略枚举 */
    public enum FusionStrategy {
        /** 线性加权融合 */
        LINEAR_WEIGHTED,
        /** 排名融合（RRF - Reciprocal Rank Fusion） */
        RANK_FUSION,
        /** 语义相似度融合 */
        SEMANTIC_FUSION,
        /** 自适应融合 */
        ADAPTIVE_FUSION
    }

    /** 执行混合搜索结果融合
     * 
     * @param vectorResults 向量搜索结果
     * @param graphEntities 图谱实体
     * @param graphRelationships 图谱关系
     * @param request 请求参数
     * @return 融合后的增强结果 */
    public List<KgEnhancedRagResponse.EnhancedResult> fuseResults(List<DocumentUnitEntity> vectorResults,
            List<GraphQueryResponse.NodeResult> graphEntities,
            List<GraphQueryResponse.RelationshipResult> graphRelationships, KgEnhancedRagRequest request) {

        long startTime = System.currentTimeMillis();

        try {
            log.debug("开始融合搜索结果，向量结果: {}, 图谱实体: {}, 图谱关系: {}", vectorResults.size(), graphEntities.size(),
                    graphRelationships.size());

            // 1. 创建基础增强结果列表
            List<KgEnhancedRagResponse.EnhancedResult> enhancedResults = createBaseResults(vectorResults);

            // 2. 为向量搜索结果关联图谱信息
            if (request.getEnableGraphEnhancement() && !graphEntities.isEmpty()) {
                associateGraphInfoWithVectorResults(enhancedResults, graphEntities, graphRelationships, request);
            }

            // 3. 添加纯图谱结果（如果启用）
            if (request.getIncludeGraphOnlyResults() && !graphEntities.isEmpty()) {
                addGraphOnlyResults(enhancedResults, graphEntities, graphRelationships, request);
            }

            // 4. 应用融合策略重新计算评分
            applyFusionStrategy(enhancedResults, request);

            log.debug("结果融合完成，生成 {} 个增强结果，耗时: {}ms", enhancedResults.size(), System.currentTimeMillis() - startTime);

            return enhancedResults;

        } catch (Exception e) {
            log.error("结果融合失败", e);
            // 返回基础向量结果作为fallback
            return createBaseResults(vectorResults);
        }
    }

    /** 创建基础增强结果列表 */
    private List<KgEnhancedRagResponse.EnhancedResult> createBaseResults(List<DocumentUnitEntity> vectorResults) {
        return vectorResults.stream().map(docUnit -> {
            KgEnhancedRagResponse.EnhancedResult result = new KgEnhancedRagResponse.EnhancedResult();
            // 转换Entity为DTO
            DocumentUnitDTO documentUnitDTO = DocumentUnitAssembler.toDTO(docUnit);
            result.setDocumentUnit(documentUnitDTO);
            result.setSourceType("VECTOR");
            // 使用similarityScore字段
            result.setVectorScore(docUnit.getSimilarityScore() != null ? docUnit.getSimilarityScore() : 0.8);
            result.setRelevanceScore(result.getVectorScore());
            result.setGraphEntities(new ArrayList<>());
            result.setGraphRelationships(new ArrayList<>());
            result.setGraphScore(0.0);
            return result;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    /** 为向量搜索结果关联图谱信息 */
    private void associateGraphInfoWithVectorResults(List<KgEnhancedRagResponse.EnhancedResult> enhancedResults,
            List<GraphQueryResponse.NodeResult> graphEntities,
            List<GraphQueryResponse.RelationshipResult> graphRelationships, KgEnhancedRagRequest request) {

        for (KgEnhancedRagResponse.EnhancedResult result : enhancedResults) {
            if (result.getDocumentUnit() == null || result.getDocumentUnit().getContent() == null) {
                continue;
            }

            String content = result.getDocumentUnit().getContent().toLowerCase();

            // 查找与文档内容相关的图谱实体
            List<GraphQueryResponse.NodeResult> relatedEntities = findRelatedEntities(content, graphEntities);
            result.setGraphEntities(relatedEntities);

            // 查找相关的关系
            Set<String> entityIds = relatedEntities.stream().map(GraphQueryResponse.NodeResult::getId)
                    .collect(Collectors.toSet());

            List<GraphQueryResponse.RelationshipResult> relatedRelationships = graphRelationships.stream().filter(
                    rel -> entityIds.contains(rel.getSourceNodeId()) || entityIds.contains(rel.getTargetNodeId()))
                    .collect(Collectors.toList());

            result.setGraphRelationships(relatedRelationships);

            // 计算图谱相关性评分
            double graphScore = calculateGraphRelevanceScore(relatedEntities, relatedRelationships, content);
            result.setGraphScore(graphScore);

            // 更新结果类型
            if (!relatedEntities.isEmpty()) {
                result.setSourceType("HYBRID");

                // 生成增强摘要
                String enhancementSummary = generateEnhancementSummary(relatedEntities, relatedRelationships);
                result.setEnhancementSummary(enhancementSummary);
            }
        }
    }

    /** 查找与内容相关的图谱实体 */
    private List<GraphQueryResponse.NodeResult> findRelatedEntities(String content,
            List<GraphQueryResponse.NodeResult> graphEntities) {
        List<GraphQueryResponse.NodeResult> relatedEntities = new ArrayList<>();

        for (GraphQueryResponse.NodeResult entity : graphEntities) {
            if (isEntityRelatedToContent(entity, content)) {
                relatedEntities.add(entity);
            }
        }

        return relatedEntities;
    }

    /** 检查图谱实体是否与文档内容相关 */
    private boolean isEntityRelatedToContent(GraphQueryResponse.NodeResult entity, String content) {
        if (entity.getProperties() == null) {
            return false;
        }

        // 检查实体名称
        Object nameObj = entity.getProperties().get("name");
        if (nameObj != null) {
            String entityName = nameObj.toString().toLowerCase();
            if (content.contains(entityName)
                    || entityName.contains(content.substring(0, Math.min(20, content.length())))) {
                return true;
            }
        }

        // 检查实体描述
        Object descObj = entity.getProperties().get("description");
        if (descObj != null) {
            String entityDesc = descObj.toString().toLowerCase();
            // 简单的关键词匹配
            String[] contentWords = content.split("\\s+");
            String[] descWords = entityDesc.split("\\s+");

            int matchCount = 0;
            for (String contentWord : contentWords) {
                if (contentWord.length() > 2) {
                    for (String descWord : descWords) {
                        if (descWord.contains(contentWord) || contentWord.contains(descWord)) {
                            matchCount++;
                            break;
                        }
                    }
                }
            }

            // 如果有足够的关键词匹配，认为相关
            return matchCount >= Math.min(3, contentWords.length / 3);
        }

        return false;
    }

    /** 计算图谱相关性评分 */
    private double calculateGraphRelevanceScore(List<GraphQueryResponse.NodeResult> entities,
            List<GraphQueryResponse.RelationshipResult> relationships, String content) {
        if (entities.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;

        // 实体数量贡献（有递减效应）
        score += Math.min(entities.size() * 0.1, 0.4);

        // 关系数量贡献
        score += Math.min(relationships.size() * 0.05, 0.3);

        // 实体名称匹配程度
        for (GraphQueryResponse.NodeResult entity : entities) {
            if (entity.getProperties() != null) {
                Object nameObj = entity.getProperties().get("name");
                if (nameObj != null) {
                    String entityName = nameObj.toString().toLowerCase();
                    if (content.contains(entityName)) {
                        score += 0.2; // 精确匹配加分
                    } else if (entityName.length() > 2
                            && content.contains(entityName.substring(0, entityName.length() - 1))) {
                        score += 0.1; // 部分匹配加分
                    }
                }
            }
        }

        return Math.min(score, 1.0); // 限制最大评分为1.0
    }

    /** 生成增强摘要 */
    private String generateEnhancementSummary(List<GraphQueryResponse.NodeResult> entities,
            List<GraphQueryResponse.RelationshipResult> relationships) {
        if (entities.isEmpty()) {
            return null;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("关联实体: ");

        List<String> entityNames = entities.stream().map(entity -> {
            if (entity.getProperties() != null && entity.getProperties().get("name") != null) {
                return entity.getProperties().get("name").toString();
            }
            return entity.getId();
        }).limit(3) // 最多显示3个实体
                .collect(Collectors.toList());

        summary.append(String.join(", ", entityNames));

        if (entities.size() > 3) {
            summary.append("等").append(entities.size()).append("个实体");
        }

        if (!relationships.isEmpty()) {
            summary.append("；关联关系: ").append(relationships.size()).append("个");
        }

        return summary.toString();
    }

    /** 添加纯图谱结果 */
    private void addGraphOnlyResults(List<KgEnhancedRagResponse.EnhancedResult> enhancedResults,
            List<GraphQueryResponse.NodeResult> graphEntities,
            List<GraphQueryResponse.RelationshipResult> graphRelationships, KgEnhancedRagRequest request) {

        // 找出重要的图谱实体（没有在向量结果中出现的）
        Set<String> existingEntityIds = enhancedResults.stream().flatMap(result -> result.getGraphEntities().stream())
                .map(GraphQueryResponse.NodeResult::getId).collect(Collectors.toSet());

        List<GraphQueryResponse.NodeResult> uniqueEntities = graphEntities.stream()
                .filter(entity -> !existingEntityIds.contains(entity.getId())).limit(5) // 最多添加5个纯图谱结果
                .collect(Collectors.toList());

        for (GraphQueryResponse.NodeResult entity : uniqueEntities) {
            KgEnhancedRagResponse.EnhancedResult graphResult = new KgEnhancedRagResponse.EnhancedResult();
            graphResult.setSourceType("GRAPH");
            graphResult.setGraphEntities(List.of(entity));
            graphResult.setVectorScore(0.0);

            // 查找该实体的关系
            List<GraphQueryResponse.RelationshipResult> entityRelationships = graphRelationships.stream().filter(
                    rel -> rel.getSourceNodeId().equals(entity.getId()) || rel.getTargetNodeId().equals(entity.getId()))
                    .collect(Collectors.toList());

            graphResult.setGraphRelationships(entityRelationships);

            // 计算纯图谱结果的评分
            double graphScore = 0.6 + Math.min(entityRelationships.size() * 0.1, 0.3);
            graphResult.setGraphScore(graphScore);
            graphResult.setRelevanceScore(graphScore);

            // 生成增强摘要
            String enhancementSummary = generateEnhancementSummary(List.of(entity), entityRelationships);
            graphResult.setEnhancementSummary(enhancementSummary);

            enhancedResults.add(graphResult);
        }
    }

    /** 应用融合策略重新计算评分 */
    private void applyFusionStrategy(List<KgEnhancedRagResponse.EnhancedResult> results, KgEnhancedRagRequest request) {
        FusionStrategy strategy = determineFusionStrategy(request);

        switch (strategy) {
            case LINEAR_WEIGHTED :
                applyLinearWeightedFusion(results, request.getGraphWeight());
                break;
            case RANK_FUSION :
                applyRankFusion(results);
                break;
            case SEMANTIC_FUSION :
                applySemanticFusion(results);
                break;
            case ADAPTIVE_FUSION :
                applyAdaptiveFusion(results, request);
                break;
        }
    }

    /** 确定融合策略 */
    private FusionStrategy determineFusionStrategy(KgEnhancedRagRequest request) {
        // 简单策略：根据图谱权重选择
        if (request.getGraphWeight() != null && request.getGraphWeight() > 0.5) {
            return FusionStrategy.SEMANTIC_FUSION;
        } else if (request.getGraphWeight() != null && request.getGraphWeight() < 0.2) {
            return FusionStrategy.LINEAR_WEIGHTED;
        } else {
            return FusionStrategy.ADAPTIVE_FUSION;
        }
    }

    /** 线性加权融合 */
    private void applyLinearWeightedFusion(List<KgEnhancedRagResponse.EnhancedResult> results, Double graphWeight) {
        double vectorWeight = 1.0 - (graphWeight != null ? graphWeight : 0.3);
        double actualGraphWeight = graphWeight != null ? graphWeight : 0.3;

        for (KgEnhancedRagResponse.EnhancedResult result : results) {
            double vectorScore = result.getVectorScore() != null ? result.getVectorScore() : 0.0;
            double graphScore = result.getGraphScore() != null ? result.getGraphScore() : 0.0;

            double fusedScore = vectorWeight * vectorScore + actualGraphWeight * graphScore;
            result.setRelevanceScore(fusedScore);
        }
    }

    /** 排名融合（RRF） */
    private void applyRankFusion(List<KgEnhancedRagResponse.EnhancedResult> results) {
        // 按向量评分排序
        List<KgEnhancedRagResponse.EnhancedResult> vectorSorted = new ArrayList<>(results);
        vectorSorted.sort((a, b) -> Double.compare(b.getVectorScore() != null ? b.getVectorScore() : 0.0,
                a.getVectorScore() != null ? a.getVectorScore() : 0.0));

        // 按图谱评分排序
        List<KgEnhancedRagResponse.EnhancedResult> graphSorted = new ArrayList<>(results);
        graphSorted.sort((a, b) -> Double.compare(b.getGraphScore() != null ? b.getGraphScore() : 0.0,
                a.getGraphScore() != null ? a.getGraphScore() : 0.0));

        // 应用RRF公式
        Map<KgEnhancedRagResponse.EnhancedResult, Double> rrfScores = new HashMap<>();
        double k = 60.0; // RRF常数

        for (KgEnhancedRagResponse.EnhancedResult result : results) {
            double rrfScore = 0.0;

            int vectorRank = vectorSorted.indexOf(result) + 1;
            int graphRank = graphSorted.indexOf(result) + 1;

            rrfScore += 1.0 / (k + vectorRank);
            rrfScore += 1.0 / (k + graphRank);

            rrfScores.put(result, rrfScore);
        }

        // 归一化RRF评分
        double maxRrfScore = rrfScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        for (KgEnhancedRagResponse.EnhancedResult result : results) {
            double normalizedScore = rrfScores.get(result) / maxRrfScore;
            result.setRelevanceScore(normalizedScore);
        }
    }

    /** 语义相似度融合 */
    private void applySemanticFusion(List<KgEnhancedRagResponse.EnhancedResult> results) {
        for (KgEnhancedRagResponse.EnhancedResult result : results) {
            double vectorScore = result.getVectorScore() != null ? result.getVectorScore() : 0.0;
            double graphScore = result.getGraphScore() != null ? result.getGraphScore() : 0.0;

            double fusedScore;

            // 对于纯图谱结果，使用更高的基础分数
            if ("GRAPH".equals(result.getSourceType())) {
                // 纯图谱结果：以图谱分数为主，给予高置信度
                fusedScore = Math.max(graphScore, 0.6); // 确保至少0.6的分数
                fusedScore = Math.min(fusedScore + 0.2, 1.0); // 给予额外加成
            } else {
                // 向量结果或混合结果：传统语义融合
                double semanticBoost = graphScore * 0.2; // 图谱信息提供语义增强
                fusedScore = vectorScore + semanticBoost;

                // 如果有图谱信息，给予额外的置信度加成
                if (result.getGraphEntities() != null && !result.getGraphEntities().isEmpty()) {
                    fusedScore = Math.min(fusedScore + 0.1, 1.0);
                }
            }

            result.setRelevanceScore(fusedScore);
        }
    }

    /** 自适应融合 */
    private void applyAdaptiveFusion(List<KgEnhancedRagResponse.EnhancedResult> results, KgEnhancedRagRequest request) {
        // 根据结果特征自适应选择融合策略
        long graphEnhancedCount = results.stream()
                .filter(r -> r.getGraphEntities() != null && !r.getGraphEntities().isEmpty()).count();

        double graphRatio = (double) graphEnhancedCount / results.size();

        if (graphRatio > 0.7) {
            // 大部分结果有图谱增强，使用语义融合
            applySemanticFusion(results);
        } else if (graphRatio > 0.3) {
            // 部分结果有图谱增强，使用排名融合
            applyRankFusion(results);
        } else {
            // 少量图谱增强，使用线性加权
            applyLinearWeightedFusion(results, 0.2);
        }
    }
}
