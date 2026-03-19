package com.hyj.aicodehelper.init;//package com.hyj.aicodehelper.init;
//
//import dev.langchain4j.model.chat.ChatModel;
//import dev.langchain4j.model.chat.StreamingChatModel;
//import dev.langchain4j.model.chat.listener.ChatModelListener;
//import dev.langchain4j.model.openai.OpenAiChatModel;
//import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
//import jakarta.annotation.Resource;
//import lombok.Data;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//
//import java.time.Duration;
//import java.util.List;
//
///**
// * Ollama本地模型配置 - DeepSeek R1
// */
//@Data
//@Configuration
//@ConfigurationProperties(prefix = "langchain4j.chat-gpt.chat-model")
//public class ChatGptModelConfig {
//
//    private String modelName = "gpt-4";  // 默认使用gpt-4，也可以是gpt-4-turbo、gpt-3.5-turbo等
//    private String apiKey;
//    private String baseUrl = "https://api.openai.com/v1";  // OpenAI官方API地址
//    private Duration timeout = Duration.ofSeconds(60);
//    // 温度参数，默认0.7
//    private Double temperature = 0.8;
//    // 最大生成token数，默认4096
//    private Integer maxTokens = 4096;
//
//    // 新增参数，根据需求配置
//    private Double topP = 1.0;
//    // 最大重试次数，默认3次
//    private Integer maxRetries = 3;
//    // 存在惩罚项，默认0.0
//    private Double presencePenalty = 0.0;
//    // 频率惩罚项，默认0.0
//    private Double frequencyPenalty = 0.0;
//
//    @Resource
//    private ChatModelListener chatModelListener;
//
//    /**
//     * 普通聊天模型（非流式）
//     * 使用@Primary注解使其成为默认的ChatModel Bean
//     */
//    @Bean
//    @Primary  // 设置为默认的ChatModel，如果有多个ChatModel Bean
//    public ChatModel chatGPTChatModel() {
//        return OpenAiChatModel.builder()
//                .apiKey(apiKey)
//                .baseUrl(baseUrl)
//                .modelName(modelName)
//                .timeout(timeout)
//                .temperature(temperature)
//                .topP(topP)
//                .maxTokens(maxTokens)
//                .maxRetries(maxRetries)
//                .presencePenalty(presencePenalty)
//                .frequencyPenalty(frequencyPenalty)
//                .listeners(List.of(chatModelListener))
//                .build();
//    }
//
//    /**
//     * 流式聊天模型
//     * 使用@Primary注解使其成为默认的StreamingChatModel Bean
//     */
//    @Bean
//    @Primary  // 设置为默认的StreamingChatModel
//    public StreamingChatModel chatGPTStreamingChatModel() {
//        return OpenAiStreamingChatModel.builder()
//                .modelName(modelName)
//                .apiKey(apiKey)
//                .baseUrl(baseUrl)
//                .timeout(timeout)
//                .temperature(temperature)
//                .topP(topP)
//                .maxTokens(maxTokens)
////                .maxTokens(maxRetries)
//                .presencePenalty(presencePenalty)
//                .frequencyPenalty(frequencyPenalty)
//                .listeners(List.of(chatModelListener))
//                .build();
//    }
//
//    /**
//     * 如果有多个模型实例，可以创建带限定符的Bean
//     */
//    @Bean(name = "gpt4ChatModel")
//    public ChatModel gpt4ChatModel() {
//        return OpenAiChatModel.builder()
//                .apiKey(apiKey)
//                .baseUrl(baseUrl)
//                .modelName("gpt-4")  // 固定使用GPT-4
//                .timeout(timeout)
//                .temperature(temperature)
//                .maxTokens(maxTokens)
//                .listeners(List.of(chatModelListener))
//                .build();
//    }
//
//    @Bean(name = "gpt35ChatModel")
//    public ChatModel gpt35ChatModel() {
//        return OpenAiChatModel.builder()
//                .apiKey(apiKey)
//                .baseUrl(baseUrl)
//                .modelName("gpt-3.5-turbo")  // 固定使用GPT-3.5
//                .timeout(timeout)
//                .temperature(temperature)
//                .maxTokens(maxTokens)
//                .listeners(List.of(chatModelListener))
//                .build();
//    }
//
//}