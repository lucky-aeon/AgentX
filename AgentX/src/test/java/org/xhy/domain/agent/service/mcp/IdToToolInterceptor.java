// File: src/main/java/org/xhy/domain/agent/service/IdToToolInterceptor.java
package org.xhy.domain.agent.service.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 拦截出站 JSON‑RPC 请求，将 id → 真正的工具名 映射保存起来 */
public class IdToToolInterceptor implements Interceptor {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Map<Long, String> idToTool = new ConcurrentHashMap<>();

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if ("POST".equalsIgnoreCase(request.method()) && request.body() != null) {
            Buffer buf = new Buffer();
            request.body().writeTo(buf);
            String json = buf.readUtf8();
            try {
                JsonNode root = MAPPER.readTree(json);
                if (root.has("id")) {
                    long id = root.get("id").asLong();
                    JsonNode params = root.path("params");
                    String toolName;
                    if (params.has("tool")) {
                        toolName = params.get("tool").asText();
                    } else if (params.has("name")) {
                        toolName = params.get("name").asText();
                    } else {
                        toolName = root.path("method").asText();
                    }
                    idToTool.put(id, toolName);
                }
            } catch (JsonProcessingException ignored) {
                // 非 JSON 或解析失败可忽略
            }
        }
        return chain.proceed(request);
    }

    /** 根据 id 获取工具名，fallback 为 "#<id>" */
    public String toolNameForId(long id) {
        return idToTool.getOrDefault(id, "#" + id);
    }
}
