package org.xhy.domain.rag.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.xhy.domain.rag.model.VectorStoreResult;
import org.xhy.infrastructure.repository.MyBatisPlusExtRepository;

import java.util.List;

/** vector_store表数据访问接口 提供向量检索和关键词检索的统一数据访问
 * 
 * @author claude */
@Mapper
public interface VectorStoreRepository extends MyBatisPlusExtRepository<VectorStoreResult> {

    /** 基于PostgreSQL全文搜索的关键词检索 使用中文分词配置和ts_rank_cd进行相关性排序
     * 
     * @param dataSetIds 数据集ID列表
     * @param userQuery 用户查询问题
     * @param maxResults 最大返回结果数量
     * @return 关键词检索结果列表，按相关性排序 */
    @Select({"<script>", "SELECT ", "    embedding_id,", "    text,", "    metadata,", "    ts_rank_cd(",
            "        to_tsvector('chinese_cfg', text),", "        to_tsquery(", "            'chinese_cfg',",
            "            replace(plainto_tsquery('chinese_cfg', #{userQuery})::text, '&amp;', '|')", "        )",
            "    ) AS score", "FROM", "    vector_store", "WHERE", "    (metadata ->> 'DATA_SET_ID') IN",
            "    <foreach collection='dataSetIds' item='dataSetId' open='(' separator=',' close=')'>",
            "        #{dataSetId}", "    </foreach>", "    AND", "    to_tsvector('chinese_cfg', text) @@ to_tsquery(",
            "        'chinese_cfg',",
            "        replace(plainto_tsquery('chinese_cfg', #{userQuery})::text, '&amp;', '|')", "    )", "ORDER BY",
            "    score DESC", "LIMIT #{maxResults}", "</script>"})
    List<VectorStoreResult> keywordSearch(@Param("dataSetIds") List<String> dataSetIds,
            @Param("userQuery") String userQuery, @Param("maxResults") Integer maxResults);
}