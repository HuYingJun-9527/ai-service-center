package com.hyj.aicodehelper.rag;

import com.hyj.aicodehelper.rag.config.DocumentPreprocessor;
import com.hyj.aicodehelper.rag.store.RedisEmbeddingStore;
import com.hyj.aicodehelper.utils.RedisUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.IngestionResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Nomic Embedding 模型专用的 RAG 配置
 * 使用 nomic-embed-text 模型
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "langchain4j.ollama.embedding-model")
public class NomicRagConfig {

    private String modelName;
    private String qwenModelName;
    private String baseUrl;
    private Integer timeout;
    private Boolean logRequests;
    private Boolean logResponses;

    // 文档路径配置
    private static final String DOCS_PATH = "src/main/resources/docse";
    // Redis键前缀，用于存储文档哈希值
    private static final String DOCUMENT_HASH_PREFIX = "doc_hash:";

    /**
     * 创建专用的 Nomic Embedding Model Bean
     * 使用 @Primary 注解，当有多个 EmbeddingModel Bean 时优先使用这个
     */
    @Bean("nomicEmbeddingModel")
    @Primary
    public EmbeddingModel nomicEmbeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(java.time.Duration.ofSeconds(timeout))
                .maxRetries(2)
                .logRequests(logRequests)
                .logResponses(logResponses).build();
    }

    /**
     * 创建专用的 Redis Embedding Store Bean
     */
    @Bean("nomicEmbeddingStore")
    @Primary
    public EmbeddingStore<TextSegment> nomicEmbeddingStore(RedisUtil redisUtil) {
        log.info("创建Nomic专用的Redis嵌入向量存储");
        RedisEmbeddingStore redisEmbeddingStore = new RedisEmbeddingStore(redisUtil);

        // 可选：清空之前的存储（开发环境使用）
//         redisEmbeddingStore.clear();

        long currentSize = redisEmbeddingStore.size();
        log.info("Redis嵌入向量存储当前大小: {} 个向量", currentSize);

        return redisEmbeddingStore;

        // 使用内存存储，可以根据需要替换为其他存储（如Redis、PGVector等）
//        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 创建文档预处理服务
     */
    @Bean
    public DocumentPreprocessor documentPreprocessor() {
        return new DocumentPreprocessor();
    }

    /**
     * 创建嵌入向量存储注入器，使用预处理服务
     */
    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor(
            @Qualifier("nomicEmbeddingModel") EmbeddingModel embeddingModel,
            @Qualifier("nomicEmbeddingStore") EmbeddingStore<TextSegment> embeddingStore,
            DocumentPreprocessor documentPreprocessor) {

        return EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .documentSplitter(new DocumentSplitter() {
                    @Override
                    public List<TextSegment> split(Document document) {
                        return documentPreprocessor.preprocessDocument(document);
                    }

                    @Override
                    public List<TextSegment> splitAll(List<Document> documents) {
                        return documentPreprocessor.preprocessDocuments(documents);
                    }
                })
                .build();
    }

    /**
     * 创建 Nomic 专用的内容检索器 Bean
     * 修改为手动触发模式，应用启动时不自动加载文档
     */
    @Bean("nomicContentRetriever")
    public ContentRetriever nomicContentRetriever(@Qualifier("nomicEmbeddingModel") EmbeddingModel embeddingModel,
                                                  @Qualifier("nomicEmbeddingStore") EmbeddingStore<TextSegment> embeddingStore) {

        log.info("Nomic RAG: 创建内容检索器（手动模式）");
        // 直接创建内容检索器，不进行自动文档加载
        return createContentRetriever(embeddingStore, embeddingModel);
    }

    /**
     * 创建内容检索器的公共方法
     */
    private ContentRetriever createContentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder().embeddingStore(embeddingStore).embeddingModel(embeddingModel).maxResults(5)           // 最大返回结果数
                .minScore(0.5)          // 最小相似度得分（0-1之间）
                .dynamicMaxResults(query -> {
                    // 例如：短查询返回较少结果，长查询返回较多结果
                    String queryText = query.text();
                    int queryLength = queryText.length();
                    // 根据查询长度动态调整返回结果数
                    if (queryLength < 10) {
                        return 3; // 短查询返回3个结果
                    } else if (queryLength < 30) {
                        return 5; // 中等长度查询返回5个结果
                    } else {
                        return 7; // 长查询返回7个结果
                    }
                }).build();

        log.info("Nomic RAG 配置完成，使用 nomic-embed-text 模型和 Redis 存储");
        return retriever;
    }
}