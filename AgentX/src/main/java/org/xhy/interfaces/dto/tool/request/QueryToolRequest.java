package org.xhy.interfaces.dto.tool.request;


import org.xhy.interfaces.dto.Page;

public class QueryToolRequest extends Page {

    private String toolName;

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
}
