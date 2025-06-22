package org.xhy.interfaces.dto.tool.request;

import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.interfaces.dto.Page;

/** 工具查询请求 */
public class QueryToolRequest extends Page {

    /** 搜索关键词（工具名称、描述） */
    private String keyword;

    /** 工具状态筛选 */
    private ToolStatus status;

    /** 是否为官方工具 */
    private Boolean isOffice;

    /** 兼容原有字段 */
    private String toolName;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public ToolStatus getStatus() {
        return status;
    }

    public void setStatus(ToolStatus status) {
        this.status = status;
    }

    public Boolean getIsOffice() {
        return isOffice;
    }

    public void setIsOffice(Boolean isOffice) {
        this.isOffice = isOffice;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
}
