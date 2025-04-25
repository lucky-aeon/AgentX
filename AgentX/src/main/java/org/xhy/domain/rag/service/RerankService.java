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
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisProperties.Embedded;
import org.springframework.stereotype.Service;
import org.xhy.domain.rag.dto.req.RerankRequest;
import org.xhy.domain.rag.dto.resp.RerankResponse;
import org.xhy.infrastructure.rag.config.EmbeddingProperties;
import org.xhy.infrastructure.rag.config.RerankProperties;

/**
 * @author shilong.zang
 * @date 16:11 <br/>
 */
@Service
public class RerankService {

    @Resource
    private RerankProperties rerankProperties;

    public List<EmbeddingMatch<TextSegment>> rerankDocument(EmbeddingSearchResult<TextSegment> textSegmentEmbeddingSearchResult,String question) {

        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();

        final List<String> list = textSegmentEmbeddingSearchResult.matches().stream().map(text -> text.embedded().text()).toList();

        final RerankRequest rerankRequest = new RerankRequest();
        rerankRequest.setModel(rerankProperties.getModel());
        rerankRequest.setQuery(question);
        rerankRequest.setDocuments(list);

        final HttpRequest build = HttpRequest.builder()
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Authorization", rerankProperties.getApiKey())
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
