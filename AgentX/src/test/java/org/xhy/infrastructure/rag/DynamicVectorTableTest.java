package org.xhy.infrastructure.rag;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.xhy.infrastructure.rag.factory.EmbeddingModelFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

/**
 * 动态向量表测试类
 * 用于验证基于嵌入模型维度动态创建向量表的可行性
 * 
 * @author xhy
 */
public class DynamicVectorTableTest {
    
    // 数据库配置 - 请根据实际环境修改
    private static final String DB_HOST = "127.0.0.1";
    private static final int DB_PORT = 5432;
    private static final String DB_NAME = "agentx";
    private static final String DB_USER = "agentx";
    private static final String DB_PASSWORD = "";
    
    // EmbeddingStore缓存
    private static final Map<String, EmbeddingStore<TextSegment>> storeCache = new ConcurrentHashMap<>();
    
    // 嵌入模型工厂实例
    private static final EmbeddingModelFactory embeddingModelFactory = new EmbeddingModelFactory();
    
    public static void main(String[] args) {
        System.out.println("=== 动态向量表测试开始 ===\n");
        
        try {
            // 测试1: 不同维度模型的基本功能
            testBasicFunctionality();
            
            // 测试2: 多模型并发使用
//            testMultiModelConcurrent();
            
            // 测试3: 缓存机制验证
//            testCacheValidation();
            
        } catch (Exception e) {
            System.err.println("测试执行出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== 动态向量表测试结束 ===");
    }
    
    /**
     * 测试基本功能
     */
    private static void testBasicFunctionality() {
        System.out.println("=== 测试1: 基本功能测试 ===");
        
        // 测试不同维度的模型
        testWithModel("Qwen/Qwen3-Embedding-8B", "", "https://api.siliconflow.cn/v1", "测试OpenAI Ada模型");
        testWithModel("Qwen/Qwen3-Embedding-4B", "", "https://api.siliconflow.cn/v1", "测试OpenAI 3-large模型");
        testWithModel("netease-youdao/bce-embedding-base_v1", "", "https://api.siliconflow.cn/v1", "测试BGE中文模型");
        
        System.out.println("基本功能测试完成\n");
    }
    
    /**
     * 测试多模型并发使用
     */
    private static void testMultiModelConcurrent() {
        System.out.println("=== 测试2: 多模型并发测试 ===");
        
        // 创建三个并发任务，模拟不同用户使用不同嵌入模型
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> 
            testWithModel("concurrent-model-1024", "key1", "url1", "并发测试-1024维度模型"));
            
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> 
            testWithModel("concurrent-model-1536", "key2", "url2", "并发测试-1536维度模型"));
            
        CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> 
            testWithModel("concurrent-model-3072", "key3", "url3", "并发测试-3072维度模型"));
        
        // 等待所有任务完成
        CompletableFuture.allOf(future1, future2, future3).join();
        
        System.out.println("并发测试完成\n");
    }
    
    /**
     * 测试缓存机制
     */
    private static void testCacheValidation() {
        System.out.println("=== 测试3: 缓存机制验证 ===");
        
        System.out.println("缓存的EmbeddingStore数量: " + storeCache.size());
        System.out.println("缓存键列表:");
        storeCache.keySet().forEach(key -> System.out.println("  - " + key));
        
        // 测试相同模型的重复调用是否复用缓存
        System.out.println("\n重复调用相同模型测试:");
        testWithModel("cache-test-model", "cache-key", "cache-url", "缓存测试第一次");
        int cacheSize1 = storeCache.size();
        testWithModel("cache-test-model", "cache-key", "cache-url", "缓存测试第二次");
        int cacheSize2 = storeCache.size();
        
        System.out.println("第一次调用后缓存数量: " + cacheSize1);
        System.out.println("第二次调用后缓存数量: " + cacheSize2);
        System.out.println("缓存复用" + (cacheSize1 == cacheSize2 ? "成功" : "失败"));
        
        System.out.println("缓存验证完成\n");
    }
    
    /**
     * 测试指定模型的完整流程
     */
    private static void testWithModel(String modelName, String apiKey, String baseUrl, String description) {
        System.out.println("--- " + description + " ---");
        System.out.println("模型: " + modelName);
        
        try {
            // 1. 创建嵌入模型配置
            EmbeddingModelFactory.EmbeddingConfig config = new EmbeddingModelFactory.EmbeddingConfig(
                apiKey, baseUrl, modelName
            );
            
            // 2. 检测模型维度
            int dimension = detectModelDimension(config);
            System.out.println("检测到维度: " + dimension);
            
            // 3. 创建/获取动态向量表
            String tableName = "vector_store_" + dimension;
            EmbeddingStore<TextSegment> store = getOrCreateEmbeddingStore(tableName, dimension);
            System.out.println("使用向量表: " + tableName);
            
            // 4. 测试向量存储和检索
            testVectorOperations(store, config, modelName, description);
            
            System.out.println("✅ " + description + " 测试成功");
            
        } catch (Exception e) {
            System.err.println("❌ " + description + " 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
    }
    
    /**
     * 动态检测嵌入模型维度
     * 优先尝试API调用，失败则使用静态映射
     */
    private static int detectModelDimension(EmbeddingModelFactory.EmbeddingConfig config) {
        try {
            System.out.println("尝试通过API检测模型维度...");
            OpenAiEmbeddingModel model = embeddingModelFactory.createEmbeddingModel(config);
            Embedding testEmbedding = model.embed("test dimension detection").content();
            int dimension = testEmbedding.dimension();
            System.out.println("API检测成功，维度: " + dimension);
            return dimension;
            
        } catch (Exception e) {
            System.out.println("API检测失败: " + e.getMessage() + "，使用静态映射");
            
            // 静态维度映射
            return getStaticModelDimension(config.getModelName());
        }
    }
    
    /**
     * 获取模型的静态维度映射
     */
    private static int getStaticModelDimension(String modelName) {
        return switch (modelName) {
            // OpenAI 模型
            case "text-embedding-ada-002" -> 1536;
            case "text-embedding-3-small" -> 1536;
            case "text-embedding-3-large" -> 3072;
            
            // 中文模型
            case "bge-large-zh", "bge-large-en" -> 1024;
            case "embedding-2" -> 1024; // 智谱AI
            
            // 测试模型
            case "concurrent-model-1024", "cache-test-model" -> 1024;
            case "concurrent-model-1536" -> 1536;
            case "concurrent-model-3072" -> 3072;
            
            // 默认维度
            default -> {
                System.out.println("未知模型 " + modelName + "，使用默认维度1024");
                yield 1024;
            }
        };
    }
    
    /**
     * 动态创建或获取EmbeddingStore实例
     */
    private static EmbeddingStore<TextSegment> getOrCreateEmbeddingStore(String tableName, int dimension) {
        String cacheKey = tableName + "_" + dimension;
        
        return storeCache.computeIfAbsent(cacheKey, key -> {
            System.out.println("创建新的EmbeddingStore: " + tableName + "，维度: " + dimension);
            
            try {
                return PgVectorEmbeddingStore.builder()
                    .table(tableName)
                    .dimension(dimension)
                    .host(DB_HOST)
                    .port(DB_PORT)
                    .user(DB_USER)
                    .password(DB_PASSWORD)
                    .database(DB_NAME)
                    .createTable(true)  // 自动创建表
                    .dropTableFirst(false)  // 不删除现有表，保留数据
                    .build();
                    
            } catch (Exception e) {
                System.err.println("创建EmbeddingStore失败: " + e.getMessage());
                throw new RuntimeException("EmbeddingStore创建失败", e);
            }
        });
    }
    
    /**
     * 测试向量存储和检索操作
     */
    private static void testVectorOperations(EmbeddingStore<TextSegment> store, 
                                           EmbeddingModelFactory.EmbeddingConfig config, 
                                           String modelName,
                                           String description) {
        try {
            // 创建嵌入模型实例
            OpenAiEmbeddingModel embeddingModel = embeddingModelFactory.createEmbeddingModel(config);
            
            // 准备测试数据
            String testText = String.format("这是%s的测试文档。该文档用于验证%s模型的向量存储和检索功能。测试时间：%d", 
                description, modelName, System.currentTimeMillis());
            
            // 创建文本段和元数据
            Metadata metadata = new Metadata();
            metadata.put("model", modelName);
            metadata.put("description", description);
            metadata.put("test_time", String.valueOf(System.currentTimeMillis()));
            metadata.put("DATA_SET_ID", "test_dataset_" + modelName);
            
            TextSegment segment = new TextSegment(testText, metadata);
            
            // 生成向量
            Embedding embedding = embeddingModel.embed(segment).content();
            System.out.println("生成向量维度: " + embedding.dimension());
            
            // 存储向量
            String embeddingId = store.add(embedding, segment);
            System.out.println("存储向量ID: " + embeddingId);
            
            // 执行相似度搜索
            testSimilaritySearch(store, embeddingModel, modelName);
            
        } catch (Exception e) {
            System.err.println("向量操作失败: " + e.getMessage());
            // 不抛出异常，继续其他测试
        }
    }
    
    /**
     * 测试相似度搜索
     */
    private static void testSimilaritySearch(EmbeddingStore<TextSegment> store, 
                                           OpenAiEmbeddingModel embeddingModel, 
                                           String modelName) {
        try {
            // 准备查询文本
            String queryText = "测试文档搜索功能 " + modelName;
            
            // 生成查询向量
            Embedding queryEmbedding = embeddingModel.embed(queryText).content();
            
            // 构建搜索请求
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.0)
                .build();
            
            // 执行搜索
            EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);
            
            System.out.println("搜索结果数量: " + searchResult.matches().size());
            
            // 输出搜索结果
            for (int i = 0; i < searchResult.matches().size(); i++) {
                EmbeddingMatch<TextSegment> match = searchResult.matches().get(i);
                String content = match.embedded().text();
                String displayContent = content.length() > 100 ? 
                    content.substring(0, 100) + "..." : content;
                    
                System.out.println(String.format("  结果%d - 相似度: %.4f, 内容: %s", 
                    i + 1, match.score(), displayContent));
            }
            
        } catch (Exception e) {
            System.err.println("搜索测试失败: " + e.getMessage());
        }
    }
}