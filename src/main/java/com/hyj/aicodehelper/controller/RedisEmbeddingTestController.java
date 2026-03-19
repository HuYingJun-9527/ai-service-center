package com.hyj.aicodehelper.controller;

import com.hyj.aicodehelper.rag.service.RedisEmbeddingManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Redis嵌入向量存储测试控制器
 */
@RestController
@RequestMapping("/api/redis-embedding")
@Tag(name = "Redis嵌入向量存储测试", description = "测试Redis嵌入向量存储功能")
@RequiredArgsConstructor
public class RedisEmbeddingTestController {

    private final RedisEmbeddingManagementService embeddingManagementService;

    @GetMapping("/stats")
    @Operation(summary = "获取嵌入向量统计信息", description = "获取Redis中嵌入向量的统计信息")
    public RedisEmbeddingManagementService.EmbeddingStats getStats() {
        return embeddingManagementService.getEmbeddingStats();
    }

    @PostMapping("/clear")
    @Operation(summary = "清空嵌入向量", description = "清空Redis中所有的嵌入向量数据")
    public String clearEmbeddings() {
        boolean success = embeddingManagementService.clearAllEmbeddings();
        return success ? "嵌入向量已清空" : "清空嵌入向量失败";
    }
}