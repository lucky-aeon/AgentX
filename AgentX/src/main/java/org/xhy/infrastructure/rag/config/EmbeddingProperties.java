package org.xhy.infrastructure.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAI嵌入服务配置属性类
 * 用于绑定application.yml中的embedding配置
 * @author zang
 */
@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {

    /**
     * 嵌入服务名称
     */
    private String name;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API URL
     */
    private String apiUrl;

    /**
     * 使用的模型名称
     */
    private String model;

    /**
     * 请求超时时间(毫秒)
     */
    private int timeout;

    /**
     * 向量存储配置
     */
    private VectorStore vectorStore = new VectorStore();

    /**
     * 向量存储配置内部类
     */
    public static class VectorStore {
        /**
         * 数据库主机地址
         */
        private String host;

        /**
         * 数据库端口
         */
        private int port = 5432;

        /**
         * 数据库用户名
         */
        private String user;

        /**
         * 数据库密码
         */
        private String password;

        /**
         * 数据库名
         */
        private String database;

        /**
         * 向量表名
         */
        private String table = "public.vector_store";

        /**
         * 向量维度
         */
        private int dimension = 1024;

        /**
         * 是否先删除表
         */
        private boolean dropTableFirst = false;

        /**
         * 是否创建表
         */
        private boolean createTable = false;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
        }

        public boolean isDropTableFirst() {
            return dropTableFirst;
        }

        public void setDropTableFirst(boolean dropTableFirst) {
            this.dropTableFirst = dropTableFirst;
        }

        public boolean isCreateTable() {
            return createTable;
        }

        public void setCreateTable(boolean createTable) {
            this.createTable = createTable;
        }
    }

    /**
     * 获取嵌入服务名称
     * @return 嵌入服务名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置嵌入服务名称
     * @param name 嵌入服务名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取API密钥
     * @return API密钥
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 设置API密钥
     * @param apiKey API密钥
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 获取API URL
     * @return API URL
     */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * 设置API URL
     * @param apiUrl API URL
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    /**
     * 获取模型名称
     * @return 模型名称
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置模型名称
     * @param model 模型名称
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取超时时间
     * @return 超时时间(毫秒)
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * 设置超时时间
     * @param timeout 超时时间(毫秒)
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * 获取向量存储配置
     * @return 向量存储配置
     */
    public VectorStore getVectorStore() {
        return vectorStore;
    }

    /**
     * 设置向量存储配置
     * @param vectorStore 向量存储配置
     */
    public void setVectorStore(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
}
