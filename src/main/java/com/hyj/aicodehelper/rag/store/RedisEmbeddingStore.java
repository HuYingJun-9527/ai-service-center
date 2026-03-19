package com.hyj.aicodehelper.rag.store;

import com.hyj.aicodehelper.utils.RedisUtil;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 自定义的Redis嵌入向量存储实现
 * 使用Redis作为持久化存储，支持嵌入向量的存储和相似度检索
 */
@Component
@Slf4j
public class RedisEmbeddingStore implements EmbeddingStore<TextSegment> {

    // Redis键前缀
    private static final String EMBEDDING_PREFIX = "embedding:";
    private static final String SEGMENT_PREFIX = "segment:";
    private static final String INDEX_KEY = "embedding_index";

    private final RedisUtil redisUtil;

    public RedisEmbeddingStore(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }


    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        add(id, embedding);
        return id;
    }

    /**
     * 实现EmbeddingStore接口的add方法
     *
     * @param id 嵌入向量ID
     * @param embedding 嵌入向量
     */
    @Override
    public void add(String id, Embedding embedding) {
        try {
            // 存储嵌入向量
            String embeddingKey = EMBEDDING_PREFIX + id;
            redisUtil.set(embeddingKey, embedding.vector());

            // 添加到索引
            redisUtil.sSet(INDEX_KEY, id);

            log.debug("嵌入向量已存储到Redis，ID: {}", id);
        } catch (Exception e) {
            log.error("存储嵌入向量到Redis失败，ID: {}", id, e);
            throw new RuntimeException("存储嵌入向量失败", e);
        }
    }

    /**
     * 实现EmbeddingStore接口的add方法
     *
     * @param embedding 嵌入向量
     * @param textSegment 文本段
     * @return 生成的ID
     */
    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = UUID.randomUUID().toString();
        add(id, embedding, textSegment);
        return id;
    }

    /**
     * 实现EmbeddingStore接口的search方法
     *
     * @param request 嵌入向量搜索请求
     * @return 嵌入向量搜索结果
     */
    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        try {
            Embedding referenceEmbedding = request.queryEmbedding();
            int maxResults = request.maxResults();
            double minScore = request.minScore();

            List<EmbeddingMatch<TextSegment>> matches = findRelevant(referenceEmbedding, maxResults, minScore);

            return new EmbeddingSearchResult<>(matches);
        } catch (Exception e) {
            log.error("嵌入向量搜索失败", e);
            throw new RuntimeException("嵌入向量搜索失败", e);
        }
    }


    public void add(String id, Embedding embedding, TextSegment textSegment) {
        try {
            // 存储嵌入向量
            String embeddingKey = EMBEDDING_PREFIX + id;
            redisUtil.set(embeddingKey, embedding.vector());

            // 存储文本段
            String segmentKey = SEGMENT_PREFIX + id;
            redisUtil.hmset(segmentKey, new ConcurrentHashMap<>(textSegment.metadata().toMap()));
            redisUtil.hset(segmentKey, "text", textSegment.text());

            // 添加到索引
            redisUtil.sSet(INDEX_KEY, id);

            log.debug("嵌入向量和文本段已存储到Redis，ID: {}", id);
        } catch (Exception e) {
            log.error("存储嵌入向量和文本段到Redis失败，ID: {}", id, e);
            throw new RuntimeException("存储嵌入向量和文本段失败", e);
        }
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>();
        for (Embedding embedding : embeddings) {
            String id = add(embedding);
            ids.add(id);
        }
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
        if (embeddings.size() != textSegments.size()) {
            throw new IllegalArgumentException("嵌入向量和文本段数量不匹配");
        }

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            String id = add(embeddings.get(i), textSegments.get(i));
            ids.add(id);
        }
        return ids;
    }

    /**
     * 从Redis检索与参考嵌入向量最相似的文本段
     *
     * @param referenceEmbedding 参考嵌入向量
     * @param maxResults         最大返回结果数量
     * @param minScore           最小相似度阈值
     * @return 包含相似度和文本段的匹配列表
     */
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        try {
            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();

            // 获取所有嵌入向量的ID
            Set<Object> ids = redisUtil.sGet(INDEX_KEY);
            if (ids == null || ids.isEmpty()) {
                return matches;
            }

            // 计算相似度并筛选
            for (Object idObj : ids) {
                String id = (String) idObj;
                // 从Redis获取存储的嵌入向量
                float[] storedVector = redisUtil.getFloatArray(EMBEDDING_PREFIX + id);

                if (storedVector != null) {
                    // 计算相似度
                    double similarity = cosineSimilarity(referenceEmbedding.vector(), storedVector);

                    if (similarity >= minScore) {
                        TextSegment segment = retrieveTextSegment(id);
                        matches.add(new EmbeddingMatch<>(similarity, id, Embedding.from(storedVector), segment));
                    }
                }
            }

            // 按相似度排序并限制结果数量
            return matches.stream()
                    .sorted((a, b) -> Double.compare(b.score(), a.score()))
                    .limit(maxResults)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("从Redis检索相似嵌入向量失败", e);
            throw new RuntimeException("检索相似嵌入向量失败", e);
        }
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 从Redis检索文本段
     *
     * @param id 文本段ID
     * @return 对应的文本段对象
     */
    private TextSegment retrieveTextSegment(String id) {
        try {
            String segmentKey = SEGMENT_PREFIX + id;
            Map<Object, Object> segmentData = redisUtil.hmget(segmentKey);

            if (segmentData == null || segmentData.isEmpty()) {
                return null;
            }

            String text = (String) segmentData.get("text");
            Map<String, String> metadata = new ConcurrentHashMap<>();

            for (Map.Entry<Object, Object> entry : segmentData.entrySet()) {
                String key = (String) entry.getKey();
                if (!"text".equals(key)) {
                    metadata.put(key, (String) entry.getValue());
                }
            }

            return TextSegment.from(text, Metadata.from(metadata));
        } catch (Exception e) {
            log.warn("检索文本段失败，ID: {}", id, e);
            return null;
        }
    }


    /**
     * 清空所有存储的嵌入向量和文本段
     */
    public void clear() {
        try {
            Set<Object> ids = redisUtil.sGet(INDEX_KEY);
            if (ids != null) {
                for (Object idObj : ids) {
                    String id = (String) idObj;
                    redisUtil.del(EMBEDDING_PREFIX + id);
                    redisUtil.del(SEGMENT_PREFIX + id);
                }
            }
            redisUtil.del(INDEX_KEY);
            log.info("Redis嵌入向量存储已清空");
        } catch (Exception e) {
            log.error("清空Redis嵌入向量存储失败", e);
        }
    }

    /**
     * 获取存储的嵌入向量数量
     */
    public long size() {
        try {
            return redisUtil.sGetSetSize(INDEX_KEY);
        } catch (Exception e) {
            log.error("获取嵌入向量数量失败", e);
            return 0;
        }
    }
}
