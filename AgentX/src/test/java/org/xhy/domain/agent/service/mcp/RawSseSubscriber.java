// File: src/main/java/org/xhy/domain/agent/service/RawSseSubscriber.java
package org.xhy.domain.agent.service.mcp;

import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

/** 并行开启一条 SSE 连接，只负责把服务器 data: 推送的原始字符串交给回调 */
public class RawSseSubscriber {

    private final EventSource eventSource;

    public RawSseSubscriber(String sseUrl, Consumer<String> onRawJson) {
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(sseUrl).build();

        EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onOpen(EventSource source, Response response) {
            }

            @Override
            public void onEvent(EventSource source, String id, String type, String data) {
                onRawJson.accept(data);
            }

            @Override
            public void onClosed(EventSource source) {
            }

            @Override
            public void onFailure(EventSource source, Throwable t, Response response) {
                t.printStackTrace();
            }
        };

        this.eventSource = EventSources.createFactory(client).newEventSource(request, listener);
    }

    /** 关闭当前 SSE 订阅 */
    public void close() {
        eventSource.cancel();
    }
}
