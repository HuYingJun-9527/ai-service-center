package com.hyj.aicodehelper.factory;

import com.hyj.aicodehelper.service.aiservice.DeepSeekServcie;
import com.hyj.aicodehelper.tools.EmailSenderTool;
import com.hyj.aicodehelper.tools.InterviewQuestionTool;
import com.hyj.aicodehelper.tools.WeatherWebTool;
import com.hyj.aicodehelper.utils.RedisUtil;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DeepseekServcieFactory {

    @Resource
    private ChatModel deepSeekChatModel;

    @Resource
    private ContentRetriever nomicContentRetriever;

    @Resource
    private McpToolProvider mcpToolProvider;

    @Resource
    private StreamingChatModel deepSeekStreamingChatModel;

    @Resource
    private EmailSenderTool emailSenderTool;

    @Resource
    private RedisUtil redisUtil;

    @Bean
    public DeepSeekServcie deepSeekServcie() {
        return AiServices.builder(DeepSeekServcie.class)
                .chatModel(deepSeekChatModel)
                .streamingChatModel(deepSeekStreamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(nomicContentRetriever)
                .tools(new InterviewQuestionTool(),emailSenderTool,new WeatherWebTool(redisUtil))
                .toolProvider(mcpToolProvider)
                .build();
    }



}
