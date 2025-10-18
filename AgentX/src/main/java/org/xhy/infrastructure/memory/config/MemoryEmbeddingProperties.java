package org.xhy.infrastructure.memory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Memory 向量存储配置属性（与 embedding.vector-store 结构一致，独立命名空间） */
@ConfigurationProperties(prefix = "memory.embedding")
public class MemoryEmbeddingProperties {

    private String name;
    private String apiKey;
    private String apiUrl;
    private String model;
    private int timeout;

    private VectorStore vectorStore = new VectorStore();

    public static class VectorStore {
        private String host;
        private int port = 5432;
        private String user;
        private String password;
        private String database;
        private String table = "public.memory_vector_store";
        private int dimension = 1024;
        private boolean dropTableFirst = false;
        private boolean createTable = true;

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

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getApiKey() {
        return apiKey;
    }
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    public String getApiUrl() {
        return apiUrl;
    }
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    public String getModel() {
        return model;
    }
    public void setModel(String model) {
        this.model = model;
    }
    public int getTimeout() {
        return timeout;
    }
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    public VectorStore getVectorStore() {
        return vectorStore;
    }
    public void setVectorStore(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
}
