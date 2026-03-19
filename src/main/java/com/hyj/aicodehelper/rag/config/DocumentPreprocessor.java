package com.hyj.aicodehelper.rag.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档预处理服务
 * 处理超长文档和Token限制问题
 */
@Service
@Slf4j
public class DocumentPreprocessor {

    private static final int MAX_SEGMENT_LENGTH = 512; // 最大段落长度（字符）
    private static final int MAX_SEGMENT_TOKENS = 384; // 最大Token数（nomic-embed-text限制）
    private static final int OVERLAP_SIZE = 50; // 重叠字符数

    /**
     * 预处理文档，确保不超过Token限制
     */
    public List<TextSegment> preprocessDocument(Document document) {
        if (document == null || document.text() == null) {
            return new ArrayList<>();
        }

        String text = document.text();
        
        // 检查文档长度
        if (text.length() <= MAX_SEGMENT_LENGTH) {
            log.debug("文档长度正常: {} 字符", text.length());
            return List.of(TextSegment.from(text, document.metadata()));
        }

        log.info("检测到超长文档: {} 字符，开始分割处理", text.length());
        
        // 使用句子分割器进行更细粒度的分割
        DocumentSplitter splitter = new DocumentBySentenceSplitter(MAX_SEGMENT_LENGTH, OVERLAP_SIZE);
        List<TextSegment> segments = splitter.split(document);
        
        // 进一步处理超长的段落
        List<TextSegment> processedSegments = new ArrayList<>();
        for (TextSegment segment : segments) {
            processedSegments.addAll(splitLongSegment(segment));
        }
        
        log.info("文档分割完成: 原始文档 {} 字符 -> {} 个段落", 
                text.length(), processedSegments.size());
        
        return processedSegments;
    }

    /**
     * 分割超长的文本段落
     */
    private List<TextSegment> splitLongSegment(TextSegment segment) {
        String text = segment.text();
        if (text.length() <= MAX_SEGMENT_LENGTH) {
            return List.of(segment);
        }

        List<TextSegment> result = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + MAX_SEGMENT_LENGTH, text.length());
            
            // 尝试在句子边界分割
            if (end < text.length()) {
                // 查找最近的句子结束位置
                int sentenceEnd = findSentenceEnd(text, start, end);
                if (sentenceEnd > start) {
                    end = sentenceEnd;
                }
            }
            
            String segmentText = text.substring(start, end).trim();
            if (!segmentText.isEmpty()) {
                result.add(TextSegment.from(segmentText, segment.metadata()));
            }
            
            start = end - OVERLAP_SIZE; // 重叠部分
            if (start <= 0) start = 0;
        }
        
        return result;
    }

    /**
     * 查找句子结束位置
     */
    private int findSentenceEnd(String text, int start, int maxEnd) {
        int end = maxEnd;
        
        // 查找最近的句子结束标点
        for (int i = maxEnd - 1; i > start; i--) {
            char c = text.charAt(i);
            if ((c == '.' || c == '。' || c == '!' || c == '！' || c == '?' || c == '？') && (i + 1 >= text.length() || Character.isWhitespace(text.charAt(i + 1)))) {
                    end = i + 1;
                    break;
                }

        }
        
        return end;
    }

    /**
     * 估算文本的Token数量（nomic-embed-text模型）
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // nomic-embed-text使用类似GPT-2的tokenizer
        // 英文约4字符=1token，中文约1.5字符=1token
        int tokenCount = 0;
        int chineseCharCount = 0;
        int englishCharCount = 0;
        
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                chineseCharCount++;
            } else if (Character.isLetterOrDigit(c)) {
                englishCharCount++;
            } else {
                tokenCount += 1; // 标点符号等
            }
        }
        
        tokenCount += (int) Math.ceil(englishCharCount / 4.0);
        tokenCount += (int) Math.ceil(chineseCharCount / 1.5);
        
        return tokenCount;
    }

    /**
     * 验证文本是否超过Token限制
     */
    public boolean isWithinTokenLimit(String text) {
        return estimateTokens(text) <= MAX_SEGMENT_TOKENS;
    }

    /**
     * 批量预处理文档
     */
    public List<TextSegment> preprocessDocuments(List<Document> documents) {
        List<TextSegment> allSegments = new ArrayList<>();
        
        for (Document document : documents) {
            allSegments.addAll(preprocessDocument(document));
        }
        
        log.info("批量文档预处理完成: {} 个文档 -> {} 个段落", 
                documents.size(), allSegments.size());
        
        return allSegments;
    }
}