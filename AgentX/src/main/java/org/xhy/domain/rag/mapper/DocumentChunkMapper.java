package org.xhy.domain.rag.mapper;

/**
 * @author shilong.zang
 * @date 18:05 <br/>
 */

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.xhy.domain.rag.model.DocumentChunkEntity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 文档块Mapper接口
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunkEntity> {

    /**
     * 使用余弦相似度查询相似文档块
     * 这里使用@Select注解方式实现向量查询
     */
    @Select("SELECT id, document_id, content, chunk_index, metadata, embedding, created_at, updated_at, " +
            "embedding <=> #{embedding} AS distance " +
            "FROM document_chunks " +
            "ORDER BY distance " +
            "LIMIT #{limit}")
    List<DocumentChunkEntity> findSimilarByCosineSimilarity(@Param("embedding") float[] embedding, @Param("limit") int limit);

    /**
     * 使用欧几里得距离查询相似文档块
     */
    @Select("SELECT id, document_id, content, chunk_index, metadata, embedding, created_at, updated_at, " +
            "embedding <-> #{embedding} AS distance " +
            "FROM document_chunks " +
            "ORDER BY distance " +
            "LIMIT #{limit}")
    List<DocumentChunkEntity> findSimilarByEuclideanDistance(@Param("embedding") float[] embedding, @Param("limit") int limit);

    /**
     * 使用内积查询相似文档块
     */
    @Select("SELECT id, document_id, content, chunk_index, metadata, embedding, created_at, updated_at, " +
            "embedding <#> #{embedding} AS distance " +
            "FROM document_chunks " +
            "ORDER BY distance " +
            "LIMIT #{limit}")
    List<DocumentChunkEntity> findSimilarByInnerProduct(@Param("embedding") float[] embedding, @Param("limit") int limit);

    /**
     * 带元数据过滤条件的相似文档块查询
     * 这里使用动态SQL示例，实际上应该在XML中实现更复杂的查询
     */
    @Select({
            "<script>",
            "SELECT id, document_id, content, chunk_index, metadata, embedding, created_at, updated_at, ",
            "embedding <=> #{embedding} AS distance ",
            "FROM document_chunks ",
            "<where>",
            "<if test='metadata != null'>",
            "<foreach collection='metadata.keys' item='key'>",
            "AND metadata-&gt;&gt;#{key} = #{metadata[${key}]} ",
            "</foreach>",
            "</if>",
            "</where>",
            "ORDER BY distance ",
            "LIMIT #{limit}",
            "</script>"
    })
    List<DocumentChunkEntity> findSimilarWithMetadata(@Param("embedding") float[] embedding,
                                                  @Param("metadata") Map<String, String> metadata,
                                                  @Param("limit") int limit);
}
