package org.xhy.domain.token.service.impl;

import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import jakarta.annotation.Resource;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.xhy.domain.rag.dto.req.RerankRequest;
import org.xhy.domain.rag.dto.resp.RerankResponse;
import org.xhy.infrastructure.rag.config.EmbeddingProperties;
import org.xhy.domain.rag.dto.req.EmbeddingReqDTO;
import org.xhy.infrastructure.rag.config.RerankProperties;

/**
 * @author shilong.zang
 * @date 14:31 <br/>
 */
@SpringBootTest

public class RagServiceImpl {

    @Resource
    private EmbeddingProperties embeddingProperties;

    @Resource
    private RerankProperties rerankProperties;


    @Test
    public void testEmbedding() {

        final EmbeddingReqDTO embeddingReqDTO = new EmbeddingReqDTO();
        embeddingReqDTO.setModel(embeddingProperties.getModel());
        embeddingReqDTO.setInput("nihao");

        final HttpRequest build = HttpRequest.builder()
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Authorization", embeddingProperties.getApiKey())
                .method(HttpMethod.POST)
                .url(embeddingProperties.getApiUrl())
                .body(JSONObject.toJSONString(embeddingReqDTO)).build();

        HttpClient httpClient = new JdkHttpClient(new JdkHttpClientBuilder());
        String response = httpClient.execute(build).body();
        System.out.println(response);

    }

    @Test
    public void testRag() {

        final RerankRequest rerankRequest = new RerankRequest();
        rerankRequest.setModel(rerankProperties.getModel());
        rerankRequest.setQuery("nihao");
        rerankRequest.setDocuments(Arrays.asList("nihaoya","wobuhao"));

        final HttpRequest build = HttpRequest.builder()
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Authorization", rerankProperties.getApiKey())
                .method(HttpMethod.POST)
                .url(rerankProperties.getApiUrl())
                .body(JSONObject.toJSONString(rerankRequest)).build();

        HttpClient httpClient = new JdkHttpClient(new JdkHttpClientBuilder());
        String response = httpClient.execute(build).body();

        final RerankResponse javaObject = JSONObject.toJavaObject(JSONObject.parseObject(response),
                RerankResponse.class);

        System.out.println(JSONObject.toJSONString(javaObject));

    }
}
