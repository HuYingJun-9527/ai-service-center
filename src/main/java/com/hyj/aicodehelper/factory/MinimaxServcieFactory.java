package com.hyj.aicodehelper.factory;

import com.hyj.aicodehelper.service.aiservice.MinimaxServcie;
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
public class MinimaxServcieFactory {

    @Resource
    private ChatModel minimaxChatModel;

    @Resource
    private StreamingChatModel minimaxStreamingChatModel;

    @Resource
    private ContentRetriever nomicContentRetriever;

    @Resource
    private McpToolProvider mcpToolProvider;

    @Resource
    private EmailSenderTool emailSenderTool;

    @Resource
    private RedisUtil redisUtil;


    @Bean
    public MinimaxServcie minimaxServcie() {
        return AiServices.builder(MinimaxServcie.class)
                .chatModel(minimaxChatModel)// 非流式模型
                .streamingChatModel(minimaxStreamingChatModel)// 流式模型
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))// 聊天内存，默认使用 MessageWindowChatMemory
                .contentRetriever(nomicContentRetriever)// 内容检索器，默认使用 NomicContentRetriever
                .tools(new InterviewQuestionTool(),emailSenderTool,new WeatherWebTool(redisUtil))// 工具，默认使用 InterviewQuestionTool、EmailSenderTool、WeatherWebTool
                .toolProvider(mcpToolProvider)// 工具提供器，默认使用 McpToolProvider
                .build();
    }



}
