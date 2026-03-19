package com.hyj.aicodehelper.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Redis嵌入向量管理服务
 * 提供嵌入向量的统计、清理等管理功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisEmbeddingManagementService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String INDEX_KEY = "embedding_index";

    /**
     * 获取嵌入向量存储统计信息
     */
    public EmbeddingStats getEmbeddingStats() {
        try {
            Long totalCount = redisTemplate.opsForSet().size(INDEX_KEY);
            long memoryUsage = getMemoryUsage();
            
            return new EmbeddingStats(
                totalCount != null ? totalCount : 0,
                memoryUsage,
                System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("获取嵌入向量统计信息失败", e);
            return new EmbeddingStats(0, 0, System.currentTimeMillis());
        }
    }

    /**
     * 清空所有嵌入向量
     */
    public boolean clearAllEmbeddings() {
        try {
            Set<Object> ids = redisTemplate.opsForSet().members(INDEX_KEY);
            if (ids != null) {
                for (Object idObj : ids) {
                    String id = (String) idObj;
                    redisTemplate.delete("embedding:" + id);
                    redisTemplate.delete("segment:" + id);
                }
            }
            redisTemplate.delete(INDEX_KEY);
            log.info("所有嵌入向量已从Redis中清除");
            return true;
        } catch (Exception e) {
            log.error("清空嵌入向量失败", e);
            return false;
        }
    }

    /**
     * 估算内存使用量（近似值）
     */
    private long getMemoryUsage() {
        try {
            // 这是一个简化的估算方法
            Long totalCount = redisTemplate.opsForSet().size(INDEX_KEY);
            if (totalCount == null || totalCount == 0) {
                return 0;
            }
            
            // 假设每个嵌入向量大约占用4KB（包含向量和元数据）
            return totalCount * 4096L;
        } catch (Exception e) {
            log.warn("估算内存使用量失败", e);
            return 0;
        }
    }

    /**
     * 嵌入向量统计信息
     */
    public static class EmbeddingStats {
        private final long totalEmbeddings;
        private final long estimatedMemoryUsage; // 字节
        private final long timestamp;

        public EmbeddingStats(long totalEmbeddings, long estimatedMemoryUsage, long timestamp) {
            this.totalEmbeddings = totalEmbeddings;
            this.estimatedMemoryUsage = estimatedMemoryUsage;
            this.timestamp = timestamp;
        }

        // Getters
        public long getTotalEmbeddings() { return totalEmbeddings; }
        public long getEstimatedMemoryUsage() { return estimatedMemoryUsage; }
        public long getTimestamp() { return timestamp; }
    }
}