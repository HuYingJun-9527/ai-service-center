package com.hyj.aicodehelper.rag;//package com.hyj.aicodehelper.rag;
//
//import dev.langchain4j.data.document.Document;
//import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
//import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
//import dev.langchain4j.data.segment.TextSegment;
//import dev.langchain4j.model.embedding.EmbeddingModel;
//import dev.langchain4j.rag.content.retriever.ContentRetriever;
//import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
//import dev.langchain4j.store.embedding.EmbeddingStore;
//import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Lazy;
//
//import java.util.Collections;
//import java.util.List;
//
///**
// * RAG 配置
// */
//@Configuration
//@Slf4j
//public class RagConfig {
//
//    @Resource
//    private EmbeddingModel embeddingModel;
//
//    @Resource
//    private EmbeddingStore<TextSegment> embeddingStore;
//
//    @Bean
//    @Lazy
//    public ContentRetriever contentRetrieverd() {
//        try {
//            // 加载文档
//            List<Document> document = FileSystemDocumentLoader.loadDocuments("src/main/resources/docs");
//
//            // 文档切割：将每个文档按每段进行分割，最大 1000 字符，每次重叠最多 200 个字符
//            DocumentByParagraphSplitter paragraphSplitter = new DocumentByParagraphSplitter(1000, 200);
//
//            // 自定义文档加载器
//            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
//                    .documentSplitter(paragraphSplitter)
//                    .textSegmentTransformer(
//                            textSegment -> TextSegment.from(
//                                    textSegment.metadata().getString("file_name") + "\n" + textSegment.text(),
//                                    textSegment.metadata()
//                            ))
//                    // 使用指定的向量模型
//                    .embeddingModel(embeddingModel)
//                    .embeddingStore(embeddingStore)
//                    .build();
//
//            // 加载文档
//            ingestor.ingest(document);
//
//            log.info("RAG配置初始化完成，已加载 {} 个文档", document.size());
//
//            // 自定义内容查询器
//            return EmbeddingStoreContentRetriever.builder()
//                    .embeddingStore(embeddingStore)
//                    .embeddingModel(embeddingModel)
//                    .maxResults(5)
//                    .minScore(0.75)
//                    .build();
//        } catch (Exception e) {
//            throw new RuntimeException("RAG配置初始化失败", e);
//        }
//    }
//}