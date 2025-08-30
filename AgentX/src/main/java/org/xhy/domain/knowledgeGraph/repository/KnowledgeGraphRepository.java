package org.xhy.domain.knowledgeGraph.repository;

import org.xhy.application.knowledgeGraph.dto.GraphIngestionRequest;
import org.xhy.application.knowledgeGraph.dto.GraphIngestionResponse;
import org.xhy.application.knowledgeGraph.dto.GraphQueryRequest;
import org.xhy.application.knowledgeGraph.dto.GraphQueryResponse;

import java.util.Map;

/**
 * 知识图谱仓储接口
 * 定义知识图谱数据的持久化操作规范
 * 
 * @author zang
 * @since 1.0.0
 */
public interface KnowledgeGraphRepository {

    /**
     * 摄取知识图谱数据
     * 批量存储实体和关系数据到图数据库
     * 
     * @param request 图数据摄取请求，包含实体和关系数据
     * @return 摄取结果响应，包含处理统计信息
     */
    GraphIngestionResponse ingestGraphData(GraphIngestionRequest request);

    /**
     * 执行知识图谱查询
     * 根据查询条件执行图遍历查询
     * 
     * @param request 图查询请求，包含查询条件和返回定义
     * @return 查询结果响应，包含节点和关系数据
     */
    GraphQueryResponse executeQuery(GraphQueryRequest request);

    /**
     * 查找两个节点之间的路径
     * 使用最短路径算法查找节点间的连接路径
     * 
     * @param sourceNodeId 源节点ID
     * @param targetNodeId 目标节点ID
     * @param maxDepth 最大搜索深度
     * @return 查询结果响应，包含路径上的节点和关系
     */
    GraphQueryResponse findPathBetweenNodes(String sourceNodeId, String targetNodeId, Integer maxDepth);

    /**
     * 获取知识图谱统计信息
     * 统计图中的节点数量、关系数量、标签类型等信息
     * 
     * @return 统计信息Map，包含各项统计数据
     */
    Map<String, Object> getGraphStatistics();

    /**
     * 检查节点是否存在
     * 根据节点ID验证节点是否在图中存在
     * 
     * @param nodeId 节点ID
     * @return true如果节点存在，false如果不存在
     */
    boolean existsNode(String nodeId);

    /**
     * 删除指定文档的所有图数据
     * 清理与指定文档相关的所有节点和关系
     * 
     * @param documentId 文档ID
     * @return 删除的节点数量和关系数量
     */
    Map<String, Integer> deleteGraphDataByDocument(String documentId);
}
