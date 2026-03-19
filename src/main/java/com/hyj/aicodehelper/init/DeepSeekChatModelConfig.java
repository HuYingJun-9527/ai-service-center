package com.hyj.aicodehelper.init;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * DeepSeek模型配置
 * DeepSeek兼容OpenAI API格式，可以直接使用OpenAiChatModel
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.deepseek.chat-model")
@Data
public class DeepSeekChatModelConfig {

    private String modelName;
    private String apiKey;
    private String baseUrl;
    private Duration timeout = Duration.ofSeconds(60);
    private Double temperature = 0.7;
    private Integer maxTokens = 4096;

    @Resource
    private ChatModelListener chatModelListener;

    @Bean
    public ChatModel deepSeekChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .returnThinking(true)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(timeout)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .listeners(List.of(chatModelListener))
                .build();
    }

    @Bean
    public StreamingChatModel deepSeekStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .modelName(modelName)
                .returnThinking(true)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .timeout(timeout)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .listeners(List.of(chatModelListener))
                .build();
    }
}