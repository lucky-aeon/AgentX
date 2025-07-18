package org.xhy.infrastructure.rag.api;

import com.dtflys.forest.annotation.Body;
import com.dtflys.forest.annotation.Header;
import com.dtflys.forest.annotation.Headers;
import com.dtflys.forest.annotation.Post;
import com.dtflys.forest.annotation.Var;
import org.xhy.domain.rag.dto.req.RerankRequest;
import org.xhy.domain.rag.dto.resp.RerankResponse;

/**
 * Rerank API Forest接口
 *
 * @author shilong.zang
 * @date 2025-07-18
 */
public interface RerankForestApi {

    /**
     * 调用Rerank API
     *
     * @param apiUrl API地址
     * @param apiKey API密钥
     * @param rerankRequest 请求参数
     * @return Rerank响应
     */
    @Post(url = "${apiUrl}",
            headers = {
                    "Authorization: Bearer ${apiKey}",
                    "Content-Type: application/json; charset=utf-8"
            })
    RerankResponse rerank(@Var("apiUrl") String apiUrl, @Var("apiKey") String apiKey,
            @Body RerankRequest rerankRequest);
}