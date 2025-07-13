package org.xhy.application.rag.dto;

import jakarta.validation.constraints.NotBlank;
import org.xhy.interfaces.dto.Page;

/**
 * 查询文件语料请求
 * 
 * @author shilong.zang
 */
public class QueryDocumentUnitsRequest extends Page {
    
    /**
     * 文件ID
     */
    @NotBlank(message = "文件ID不能为空")
    private String fileId;
    
    /**
     * 内容关键词搜索
     */
    private String keyword;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}