package org.xhy.domain.rag.repository;

/**
 * @author shilong.zang
 * @date 18:01 <br/>
 */

import java.util.List;
import java.util.Map;

import org.xhy.domain.llm.model.ModelEntity;
import org.xhy.domain.rag.model.DocumentChunkEntity;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

/**
 * 文档块仓储接口
 * @author zang
 */
public interface DocumentChunkRepository extends MyBatisPlusExtRepository<ModelEntity> {

    /**
     * 保存文档块
     */
    void save(DocumentChunkEntity documentChunk);

    /**
     * 批量保存文档块
     */
    void batchSave(List<DocumentChunkEntity> documentChunks);

    /**
     * 根据ID查询文档块
     */
    DocumentChunkEntity findById(String id);

    /**
     * 根据文档ID查询所有文档块
     */
    List<DocumentChunkEntity> findByDocumentId(String documentId);

    /**
     * 使用余弦相似度查询相似文档块
     *
     * @param embedding 查询向量
     * @param limit 返回结果数量限制
     * @return 相似度从高到低排序的文档块列表
     */
    List<DocumentChunkEntity> findSimilarByCosineSimilarity(float[] embedding, int limit);

    /**
     * 使用欧几里得距离查询相似文档块
     *
     * @param embedding 查询向量
     * @param limit 返回结果数量限制
     * @return 距离从近到远排序的文档块列表
     */
    List<DocumentChunkEntity> findSimilarByEuclideanDistance(float[] embedding, int limit);

    /**
     * 使用内积查询相似文档块
     *
     * @param embedding 查询向量
     * @param limit 返回结果数量限制
     * @return 内积从大到小排序的文档块列表
     */
    List<DocumentChunkEntity> findSimilarByInnerProduct(float[] embedding, int limit);

    /**
     * 带元数据过滤条件的相似文档块查询
     *
     * @param embedding 查询向量
     * @param metadata 元数据过滤条件
     * @param limit 返回结果数量限制
     * @return 相似度从高到低排序的文档块列表
     */
    List<DocumentChunkEntity> findSimilarWithMetadata(float[] embedding, Map<String, String> metadata, int limit);
}
