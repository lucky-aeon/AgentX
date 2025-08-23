package org.xhy.domain.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xhy.domain.rag.constant.SearchType;
import org.xhy.domain.rag.model.VectorStoreResult;
import org.xhy.domain.rag.repository.VectorStoreRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 关键词检索领域服务 专门负责基于PostgreSQL全文搜索的关键词检索算法
 * 
 * @author claude */
@Service
public class KeywordSearchDomainService {

    private static final Logger log = LoggerFactory.getLogger(KeywordSearchDomainService.class);

    private final VectorStoreRepository vectorStoreRepository;

    public KeywordSearchDomainService(VectorStoreRepository vectorStoreRepository) {
        this.vectorStoreRepository = vectorStoreRepository;
    }

    /** 执行关键词检索 基于PostgreSQL的中文全文搜索功能，使用ts_rank_cd进行相关性排序
     * 
     * @param dataSetIds 数据集ID列表
     * @param userQuery 用户查询问题
     * @param maxResults 最大返回结果数量
     * @return 关键词检索结果列表，失败时返回空集合 */
    public List<VectorStoreResult> keywordSearch(List<String> dataSetIds, String userQuery, Integer maxResults) {
        // 参数验证
        if (dataSetIds == null || dataSetIds.isEmpty()) {
            log.warn("Dataset IDs list is empty for keyword search");
            return Collections.emptyList();
        }

        if (!StringUtils.hasText(userQuery)) {
            log.warn("User query is empty for keyword search");
            return Collections.emptyList();
        }

        if (maxResults == null || maxResults <= 0) {
            log.warn("Invalid max results: {}, using default value 20", maxResults);
            maxResults = 20;
        }

        // 记录搜索开始时间
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Starting keyword search with params: datasets={}, query='{}', maxResults={}", dataSetIds,
                    userQuery, maxResults);

            // 执行关键词检索SQL
            List<VectorStoreResult> results = vectorStoreRepository.keywordSearch(dataSetIds, userQuery, maxResults);

            // 为结果设置检索类型标识
            for (VectorStoreResult result : results) {
                result.setSearchType(SearchType.KEYWORD);
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Keyword search completed for query: '{}', returned {} documents, took {}ms", userQuery,
                    results.size(), totalTime);

            return results;

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("Error during keyword search for query: '{}', datasets: {}, time: {}ms", userQuery, dataSetIds,
                    totalTime, e);

            // 关键词检索失败时返回空集合，不影响向量检索结果
            return Collections.emptyList();
        }
    }
}