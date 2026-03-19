package com.hyj.aicodehelper.rag.service;

import com.hyj.aicodehelper.utils.RedisUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG数据管理服务
 * 负责文件上传、切片处理和查询功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagDataManagementService {

    @Qualifier("nomicEmbeddingModel")
    private final EmbeddingModel embeddingModel;

    @Qualifier("nomicEmbeddingStore")
    private final EmbeddingStore<TextSegment> embeddingStore;

    private final RedisUtil redisUtil;


    @Value("${rag.upload.temp-dir:/tmp/rag-uploads}")
    private String tempUploadDir;

    // 文档类型配置
    private static final Map<String, DocumentParser> DOCUMENT_PARSERS = Map.of(
            "md", new TextDocumentParser(),
            "txt", new TextDocumentParser(),
            "doc", new ApachePoiDocumentParser(),
            "docx", new ApachePoiDocumentParser(),
            "pdf", new ApachePdfBoxDocumentParser()
    );

    // Redis键前缀
    private static final String DOCUMENT_TYPE_PREFIX = "rag_doc_type:";
    private static final String SEGMENT_TYPE_PREFIX = "rag_segment_type:";

    /**
     * 处理文件上传和切片
     */
    public boolean processUploadedFile(MultipartFile file, String type) {
        try {
            // 验证文件类型
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            
            if (!DOCUMENT_PARSERS.containsKey(fileExtension.toLowerCase())) {
                log.warn("不支持的文件类型: {}", fileExtension);
                return false;
            }

            // 创建临时目录
            Path tempDir = Paths.get(tempUploadDir);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }

            // 保存上传文件
            Path tempFile = tempDir.resolve(UUID.randomUUID().toString() + "_" + originalFilename);
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            // 解析文档
            DocumentParser parser = DOCUMENT_PARSERS.get(fileExtension.toLowerCase());
            Document document = FileSystemDocumentLoader.loadDocument(tempFile, parser);
            
            // 设置文档元数据
            document.metadata().put("file_name", originalFilename);
            document.metadata().put("file_type", type);
            document.metadata().put("upload_time", String.valueOf(System.currentTimeMillis()));

            // 切片处理
            List<TextSegment> segments = processDocument(document, type);

            // 向量化并存储
            boolean success = storeSegments(segments, type);

            // 清理临时文件
            Files.deleteIfExists(tempFile);

            log.info("文件处理完成: {} (类型: {}), 生成 {} 个切片", originalFilename, type, segments.size());
            return success;

        } catch (Exception e) {
            log.error("处理上传文件失败: {}", file.getOriginalFilename(), e);
            return false;
        }
    }

    /**
     * 查询指定类型的切片数据
     */
    public List<Map<String, Object>> querySegmentsByType(String type) {
        try {
            // 从Redis中获取指定类型的切片ID列表
            String typeKey = SEGMENT_TYPE_PREFIX + type;
            Set<Object> segmentIds = redisUtil.sGet(typeKey);

            if (segmentIds == null || segmentIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> segments = new ArrayList<>();
            
            for (Object segmentIdObj : segmentIds) {
                String segmentId = (String) segmentIdObj;
                
                // 获取切片内容
                String segmentKey = "segment:" + segmentId;
                TextSegment segment = (TextSegment) redisUtil.get(segmentKey);
                
                if (segment != null) {
                    Map<String, Object> segmentInfo = new HashMap<>();
                    segmentInfo.put("id", segmentId);
                    segmentInfo.put("text", segment.text());
                    segmentInfo.put("metadata", segment.metadata().toMap());
                    segments.add(segmentInfo);
                }
            }

            log.info("查询类型 {} 的切片数据，找到 {} 个切片", type, segments.size());
            return segments;

        } catch (Exception e) {
            log.error("查询切片数据失败，类型: {}", type, e);
            return Collections.emptyList();
        }
    }

    /**
     * 文档切片处理
     */
    private List<TextSegment> processDocument(Document document, String type) {
        // 文档分割配置
        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(1000, 200);
        
        // 分割文档
        List<TextSegment> segments = splitter.split(document);
        
        // 添加类型信息到元数据
        return segments.stream()
                .map(segment -> {
                    // 创建新的Metadata对象
                    Metadata metadata = Metadata.from(segment.metadata().toMap());
                    metadata.put("doc_type", type);
                    return TextSegment.from(segment.text(), metadata);
                })
                .collect(Collectors.toList());
    }

    /**
     * 存储切片数据到Redis
     */
    private boolean storeSegments(List<TextSegment> segments, String type) {
        try {
            // 创建文档注入器
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            // 注入切片到向量存储
            // 将TextSegment转换为Document对象
            List<Document> documents = segments.stream()
                    .map(segment -> {
                        // 创建新的Document对象，保留元数据
                        return Document.from(segment.text(), segment.metadata());
                    })
                    .collect(Collectors.toList());
            // 注入文档到向量存储
            ingestor.ingest(documents);

            // 记录类型关联
            String typeKey = SEGMENT_TYPE_PREFIX + type;
            for (int i = 0; i < segments.size(); i++) {
                // 使用索引作为segment ID，确保唯一性
                String segmentId = type + "_" + System.currentTimeMillis() + "_" + i;
                redisUtil.sSet(typeKey, segmentId);

                // 存储segment内容到Redis，便于查询
                String segmentKey = "segment:" + segmentId;
                redisUtil.set(segmentKey, segments.get(i));
            }

            log.info("存储 {} 个切片到Redis，类型: {}", segments.size(), type);
            return true;

        } catch (Exception e) {
            log.error("存储切片数据失败，类型: {}", type, e);
            return false;
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 获取支持的文件类型列表
     */
    public Set<String> getSupportedFileTypes() {
        return DOCUMENT_PARSERS.keySet();
    }

    /**
     * 获取已存储的类型列表
     */
    public Set<String> getStoredTypes() {
        try {
            // 查找所有以SEGMENT_TYPE_PREFIX开头的键
            Set<String> keys = redisUtil.keys(SEGMENT_TYPE_PREFIX + "*");
            return keys.stream()
                    .map(key -> key.substring(SEGMENT_TYPE_PREFIX.length()))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("获取已存储类型列表失败", e);
            return Collections.emptySet();
        }
    }
}