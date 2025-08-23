package org.xhy.domain.rag.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.xhy.domain.rag.dto.req.RerankRequest;
import org.xhy.domain.rag.dto.resp.RerankResponse;
import org.xhy.infrastructure.rag.api.RerankForestApi;
import org.xhy.infrastructure.rag.config.RerankProperties;

/** @author shilong.zang
 * @date 16:11 <br/>
 */
@Service
public class RerankDomainService {

    @Resource
    private RerankProperties rerankProperties;

    @Resource
    private RerankForestApi rerankForestApi;

    /** 重排序文档列表
     * 
     * @param documents 待重排序的文档文本列表
     * @param query 查询问题
     * @return 重排序后的文档索引列表 */
    public List<Integer> rerank(List<String> documents, String query) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        if (query == null || query.trim().isEmpty()) {
            // 如果查询为空，返回原始顺序索引
            return documents.stream().map(documents::indexOf).collect(Collectors.toList());
        }

        final RerankRequest rerankRequest = new RerankRequest();
        rerankRequest.setModel(rerankProperties.getModel());
        rerankRequest.setQuery(query);
        rerankRequest.setDocuments(documents);

        try {
            // 调用Forest接口调用Rerank API
            final RerankResponse rerankResponse = rerankForestApi.rerank(rerankProperties.getApiUrl(),
                    rerankProperties.getApiKey(), rerankRequest);

            final List<RerankResponse.SearchResult> results = rerankResponse.getResults();

            return results.stream().map(RerankResponse.SearchResult::getIndex).collect(Collectors.toList());

        } catch (Exception e) {
            // 重排序失败时返回原始顺序
            return documents.stream().map(documents::indexOf).collect(Collectors.toList());
        }
    }

    /** 重排序文档（已废弃）
     * 
     * @deprecated 推荐使用 rerank(List&lt;String&gt; documents, String query) 方法 */
    @Deprecated
    public List<EmbeddingMatch<TextSegment>> rerankDocument(
            EmbeddingSearchResult<TextSegment> textSegmentEmbeddingSearchResult, String question) {

        // 提取文档文本列表
        final List<String> documents = textSegmentEmbeddingSearchResult.matches().stream()
                .map(text -> text.embedded().text()).toList();

        // 使用新的rerank方法
        final List<Integer> rerankedIndices = rerank(documents, question);

        // 根据重排序索引重新排列结果
        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        List<EmbeddingMatch<TextSegment>> originalMatches = textSegmentEmbeddingSearchResult.matches();

        rerankedIndices.forEach(index -> {
            if (index >= 0 && index < originalMatches.size()) {
                matches.add(originalMatches.get(index));
            }
        });

        return matches;
    }

}
