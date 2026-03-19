package com.hyj.aicodehelper.controller;

import com.hyj.aicodehelper.rag.service.RagDataManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RAG数据管理控制器
 * 提供文件上传、切片查询等API接口
 */
@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG数据管理", description = "RAG数据存储和检索管理接口")
@Slf4j
@RequiredArgsConstructor
public class RagDataController {

    private final RagDataManagementService ragDataManagementService;

    /**
     * 文件上传与处理接口
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(summary = "文件上传与处理", description = "接收客户端上传的文件，进行切片处理并存储到Redis")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @Parameter(description = "上传的文件", required = true, schema = @Schema(implementation = MultipartFile.class))
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "文件类型标识", required = true, example = "weather")
            @RequestParam("type") String type) {

        try {
            log.info("接收文件上传请求: {} (类型: {})", file.getOriginalFilename(), type);

            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "文件不能为空"
                ));
            }

            // 处理文件
            boolean success = ragDataManagementService.processUploadedFile(file, type);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "文件处理完成",
                        "filename", file.getOriginalFilename(),
                        "type", type
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "文件处理失败"
                ));
            }

        } catch (Exception e) {
            log.error("文件上传处理异常: {}", file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "服务器内部错误: " + e.getMessage()
            ));
        }
    }

    /**
     * 切片数据查询接口
     */
    @GetMapping("/segments")
    @Operation(summary = "查询切片数据", description = "根据指定类型查询Redis中存储的文档切片数据")
    public ResponseEntity<Map<String, Object>> querySegments(
            @Parameter(description = "查询的数据类型标识", required = true, example = "weather")
            @RequestParam("type") String type) {

        try {
            log.info("查询切片数据，类型: {}", type);

            List<Map<String, Object>> segments = ragDataManagementService.querySegmentsByType(type);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "type", type,
                    "count", segments.size(),
                    "segments", segments
            ));

        } catch (Exception e) {
            log.error("查询切片数据异常，类型: {}", type, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "查询失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取支持的文件类型列表
     */
    @GetMapping("/supported-types")
    @Operation(summary = "获取支持的文件类型", description = "获取系统支持的文件类型列表")
    public ResponseEntity<Map<String, Object>> getSupportedFileTypes() {
        try {
            Set<String> supportedTypes = ragDataManagementService.getSupportedFileTypes();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "supportedTypes", supportedTypes
            ));
        } catch (Exception e) {
            log.error("获取支持的文件类型列表异常", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取已存储的类型列表
     */
    @GetMapping("/stored-types")
    @Operation(summary = "获取已存储的类型", description = "获取Redis中已存储的文档类型列表")
    public ResponseEntity<Map<String, Object>> getStoredTypes() {
        try {
            Set<String> storedTypes = ragDataManagementService.getStoredTypes();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "storedTypes", storedTypes
            ));
        } catch (Exception e) {
            log.error("获取已存储的类型列表异常", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查RAG数据管理服务状态")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "service", "RAG Data Management",
                "status", "running",
                "timestamp", System.currentTimeMillis()
        ));
    }
}