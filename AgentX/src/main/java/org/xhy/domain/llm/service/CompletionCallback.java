package org.xhy.domain.llm.service;

/** LLM完成回调接口 用于解耦基础设施层和领域层，由应用层实现 */
public interface CompletionCallback {

    /** 处理部分响应
     *
     * @param partialResponse 部分响应内容 */
    void onPartialResponse(String partialResponse);

    /** 处理完整响应
     *
     * @param completeResponse 完整响应内容
     * @param inputTokenCount 输入token数量
     * @param outputTokenCount 输出token数量 */
    void onCompleteResponse(String completeResponse, Integer inputTokenCount, Integer outputTokenCount);

    /** 处理错误
     *
     * @param error 错误信息 */
    void onError(Throwable error);
}
