package org.xhy.application.conversation.service.message.util;

import org.xhy.application.conversation.service.handler.context.ChatContext;

/**
 * 保存当前对话的 ChatContext，便于工具执行等下游组件在同一线程内获取
 * 注意：TokenStream/工具执行可能运行在框架线程池中，InheritableThreadLocal 在池化线程场景下不保证传递；
 * 该 Holder 主要用于同步调用和同线程回调场景。
 */
public class ChatContextHolder {

    private static final InheritableThreadLocal<ChatContext> CTX = new InheritableThreadLocal<>();

    public static void set(ChatContext ctx) {
        CTX.set(ctx);
    }

    public static ChatContext get() {
        return CTX.get();
    }

    public static void clear() {
        CTX.remove();
    }
}

