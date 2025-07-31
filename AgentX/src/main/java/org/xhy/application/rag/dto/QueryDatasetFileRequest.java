package org.xhy.application.rag.dto;

import org.xhy.interfaces.dto.Page;

/** 查询数据集文件请求
 * @author shilong.zang
 * @date 2024-12-09 */
public class QueryDatasetFileRequest extends Page {

    /** 搜索关键词 */
    private String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}