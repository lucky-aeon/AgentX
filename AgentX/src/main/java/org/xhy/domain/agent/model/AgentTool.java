package org.xhy.domain.agent.model;

/** Agent工具类，表示Agent可以使用的工具 */
public class AgentTool {

    /** 工具ID */
    private String id;

    /** 工具名称 */
    private String name;

    /** 工具描述 */
    private String description;

    /** 工具类型 */
    private String type;

    /** 工具权限 */
    private String permissions;

    /** 工具配置 */
    private Object config;

    /** 无参构造函数 */
    public AgentTool() {
    }

    /** 构造函数 */
    public AgentTool(String id, String name, String description, String type, String permissions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.permissions = permissions;
    }

    /** 构造函数（带配置） */
    public AgentTool(String id, String name, String description, String type, String permissions, Object config) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.permissions = permissions;
        this.config = config;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public Object getConfig() {
        return config;
    }

    public void setConfig(Object config) {
        this.config = config;
    }
}
