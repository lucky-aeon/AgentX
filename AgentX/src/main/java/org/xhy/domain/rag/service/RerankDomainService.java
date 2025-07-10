package org.xhy.domain.rag.service;

import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.xhy.domain.rag.dto.req.RerankRequest;
import org.xhy.domain.rag.dto.resp.RerankResponse;
import org.xhy.infrastructure.rag.config.RerankProperties;

/**
 * @author shilong.zang
 * @date 16:11 <br/>
 */
@Service
public class RerankDomainService {

    @Resource
    private RerankProperties rerankProperties;

    public List<EmbeddingMatch<TextSegment>> rerankDocument(EmbeddingSearchResult<TextSegment> textSegmentEmbeddingSearchResult,String question) {

        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();

        final List<String> list = textSegmentEmbeddingSearchResult.matches().stream().map(text -> text.embedded().text()).toList();

        final RerankRequest rerankRequest = new RerankRequest();
        rerankRequest.setModel(rerankProperties.getModel());
        rerankRequest.setQuery(question);
        rerankRequest.setDocuments(list);

        // todo xhy 我觉得把这一块抽离出来做成 ReRankAPI放在 基础设施层，让 api 和业务剥离开来，这样
        // /Users/xhy/course/AgentX/AgentX/src/main/java/org/xhy/domain/rag/dto/req/resp 就可以抽离了
        // 也更利于维护， api 和业务不要强绑定，避免后续测试 api 的时候不能最小化测试
        final HttpRequest build = HttpRequest.builder()
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Authorization", "Bearer " + rerankProperties.getApiKey())
                .method(HttpMethod.POST)
                .url(rerankProperties.getApiUrl())
                .body(JSONObject.toJSONString(rerankRequest)).build();

        HttpClient httpClient = new JdkHttpClient(new JdkHttpClientBuilder());
        String response = httpClient.execute(build).body();

        final RerankResponse rerankResponse = JSONObject.toJavaObject(JSONObject.parseObject(response),
                RerankResponse.class);

        final List<RerankResponse.SearchResult> results = rerankResponse.getResults();

        results.forEach(result -> {
            final Integer index = result.getIndex();
            matches.add(textSegmentEmbeddingSearchResult.matches().get(index));
        });

        return matches;

    }

}
