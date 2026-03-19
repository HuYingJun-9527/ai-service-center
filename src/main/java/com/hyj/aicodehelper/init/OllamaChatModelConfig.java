package com.hyj.aicodehelper.init;

import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.Resource;

import java.time.Duration;
import java.util.List;

/**
 * Ollama本地模型配置 - DeepSeek R1
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "langchain4j.ollama.chat-model")
public class OllamaChatModelConfig {

    private String qwenModelName;
    private String baseUrl;
    private Integer timeout;

    @Resource
    private ChatModelListener chatModelListener;

    @PostConstruct
    public void init() {
        log.info("Qwen model name: {}", qwenModelName);
    }

    @Bean
    public StreamingChatModel myQwenChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(qwenModelName)
                .timeout(Duration.ofSeconds(timeout))// 设置请求超时时间，默认60秒
                .listeners(List.of(chatModelListener))// 注册聊天模型监听器
                .temperature(0.8)// 温度参数，控制输出的随机性，默认0.8
                .topP(0.9)//  nucleus sampling 参数，控制输出的多样性，默认0.9
//                .numPredict(2024)// 最大预测 token 数，默认1024
//                .repeatPenalty(1.2)// 重复惩罚参数，默认1.1
//                .mirostat(2)// 启用 Mirostat 2.0 算法，默认不启用
//                .stop(List.of("```", "```\n", "\n```\n"))// 停止生成的 token 列表，默认不停止
//                .mirostatEta(0.5)   // Mirostat 2.0 算法的学习率，默认0.1
//                .numCtx(4096)// 上下文窗口大小，默认2048
                .logRequests(true)// 是否记录请求日志，默认false
                .logResponses(true)// 是否记录响应日志，默认false
                .build();
    }

}