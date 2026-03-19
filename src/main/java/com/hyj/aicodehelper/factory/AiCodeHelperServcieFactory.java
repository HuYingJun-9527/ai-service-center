package com.hyj.aicodehelper.factory;

import com.hyj.aicodehelper.service.aiservice.AiCodeHelperServcie;
import com.hyj.aicodehelper.tools.EmailSenderTool;
import com.hyj.aicodehelper.tools.InterviewQuestionTool;
import com.hyj.aicodehelper.tools.WeatherWebTool;
import com.hyj.aicodehelper.utils.RedisUtil;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@Slf4j
public class AiCodeHelperServcieFactory {

//    @Resource
//    private ChatModel myQwenChatModel;

    @Resource
    private ContentRetriever nomicContentRetriever;

    @Resource
    private McpToolProvider mcpToolProvider;

    @Resource
    private StreamingChatModel myQwenChatModel;

    @Resource
    private EmailSenderTool emailSenderTool;

    @Resource
    private RedisUtil redisUtil;

    @Bean
    public AiCodeHelperServcie aiCodeHelperServcie() {
        return AiServices.builder(AiCodeHelperServcie.class)
//                .chatModel(myQwenChatModel)
                .streamingChatModel(myQwenChatModel)
                .chatMemoryProvider(memoryId -> {
                    log.info("为memoryId: {} 创建新的对话记忆", memoryId);
                    return MessageWindowChatMemory.withMaxMessages(10);
                })
//                .contentRetriever(nomicContentRetriever)
                .tools(new InterviewQuestionTool(),emailSenderTool,new WeatherWebTool(redisUtil))
                .toolProvider(mcpToolProvider)
//                .outputGuardrails(new SafeOutputGuardrail())
                .build();
    }

}
