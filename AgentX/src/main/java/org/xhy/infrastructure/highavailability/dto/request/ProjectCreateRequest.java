package org.xhy.infrastructure.highavailability.dto.request;

/** 项目创建请求
 * 
 * @author xhy
 * @since 1.0.0 */
public class ProjectCreateRequest {

    /** 项目名称 */
    private String name;

    /** 项目描述 */
    private String description;

    /** API密钥 */
    private String apiKey;

    public ProjectCreateRequest() {
    }

    public ProjectCreateRequest(String name, String description, String apiKey) {
        this.name = name;
        this.description = description;
        this.apiKey = apiKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}