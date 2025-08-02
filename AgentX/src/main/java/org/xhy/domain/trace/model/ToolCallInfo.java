package org.xhy.domain.trace.model;

/** 工具调用信息值对象 封装单次工具调用的详细信息 */
public class ToolCallInfo {

    /** 工具名称 */
    private final String toolName;

    /** MCP服务器名称 */
    private final String mcpServerName;

    /** 工具描述 */
    private final String toolDescription;

    /** 调用参数(JSON格式) */
    private final String requestArgs;

    /** 响应数据(JSON格式) */
    private final String responseData;

    /** 执行耗时(毫秒) */
    private final Integer executionTime;

    /** 是否成功 */
    private final Boolean success;

    /** 错误信息 */
    private final String errorMessage;

    /** MCP连接URL */
    private final String mcpTransportUrl;

    /** MCP连接是否成功 */
    private final Boolean mcpConnectionSuccess;

    private ToolCallInfo(Builder builder) {
        this.toolName = builder.toolName;
        this.mcpServerName = builder.mcpServerName;
        this.toolDescription = builder.toolDescription;
        this.requestArgs = builder.requestArgs;
        this.responseData = builder.responseData;
        this.executionTime = builder.executionTime;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
        this.mcpTransportUrl = builder.mcpTransportUrl;
        this.mcpConnectionSuccess = builder.mcpConnectionSuccess;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String toolName;
        private String mcpServerName;
        private String toolDescription;
        private String requestArgs;
        private String responseData;
        private Integer executionTime;
        private Boolean success = true;
        private String errorMessage;
        private String mcpTransportUrl;
        private Boolean mcpConnectionSuccess = true;

        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        public Builder mcpServerName(String mcpServerName) {
            this.mcpServerName = mcpServerName;
            return this;
        }

        public Builder toolDescription(String toolDescription) {
            this.toolDescription = toolDescription;
            return this;
        }

        public Builder requestArgs(String requestArgs) {
            this.requestArgs = requestArgs;
            return this;
        }

        public Builder responseData(String responseData) {
            this.responseData = responseData;
            return this;
        }

        public Builder executionTime(Integer executionTime) {
            this.executionTime = executionTime;
            return this;
        }

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder mcpTransportUrl(String mcpTransportUrl) {
            this.mcpTransportUrl = mcpTransportUrl;
            return this;
        }

        public Builder mcpConnectionSuccess(Boolean mcpConnectionSuccess) {
            this.mcpConnectionSuccess = mcpConnectionSuccess;
            return this;
        }

        public ToolCallInfo build() {
            return new ToolCallInfo(this);
        }
    }

    // Getter方法
    public String getToolName() {
        return toolName;
    }

    public String getMcpServerName() {
        return mcpServerName;
    }

    public String getToolDescription() {
        return toolDescription;
    }

    public String getRequestArgs() {
        return requestArgs;
    }

    public String getResponseData() {
        return responseData;
    }

    public Integer getExecutionTime() {
        return executionTime;
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMcpTransportUrl() {
        return mcpTransportUrl;
    }

    public Boolean getMcpConnectionSuccess() {
        return mcpConnectionSuccess;
    }

    @Override
    public String toString() {
        return "ToolCallInfo{" + "toolName='" + toolName + '\'' + ", mcpServerName='" + mcpServerName + '\''
                + ", executionTime=" + executionTime + ", success=" + success + ", mcpConnectionSuccess="
                + mcpConnectionSuccess + '}';
    }
}