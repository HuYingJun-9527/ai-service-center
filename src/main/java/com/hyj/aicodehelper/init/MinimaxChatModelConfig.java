package com.hyj.aicodehelper.init;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;

/**
 * Minimax模型配置
 * Minimax兼容OpenAI API格式，可以直接使用OpenAiChatModel
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.minimax.chat-model")
@Data
public class MinimaxChatModelConfig {

    private String modelName;
    private String apiKey;
    private String baseUrl;

    @Resource
    private ChatModelListener chatModelListener;


    @Bean
    public ChatModel minimaxChatModel() {
        return OpenAiChatModel.builder()
                .modelName(modelName)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .listeners(List.of(chatModelListener))
                .temperature(0.8)
                .maxTokens(3000)
                .build();
    }

    // 新增的流式模型 Bean
    @Bean
    public StreamingChatModel minimaxStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .modelName(modelName)  // 例如 "MiniMax-M2.1"
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .listeners(List.of(chatModelListener))
                .temperature(0.8)
                .maxTokens(3000)
                .build();
    }
}