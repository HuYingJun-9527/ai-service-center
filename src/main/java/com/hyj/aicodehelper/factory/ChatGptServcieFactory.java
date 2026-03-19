package com.hyj.aicodehelper.factory;//package com.hyj.aicodehelper.factory;
//
//import com.hyj.aicodehelper.service.aiservice.ChatGptServcie;
//import com.hyj.aicodehelper.tools.EmailSenderTool;
//import com.hyj.aicodehelper.tools.InterviewQuestionTool;
//import dev.langchain4j.mcp.McpToolProvider;
//import dev.langchain4j.memory.ChatMemory;
//import dev.langchain4j.memory.chat.MessageWindowChatMemory;
//import dev.langchain4j.model.chat.ChatModel;
//import dev.langchain4j.model.chat.StreamingChatModel;
//import dev.langchain4j.rag.content.retriever.ContentRetriever;
//import dev.langchain4j.service.AiServices;
//import jakarta.annotation.Resource;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//
//@Configuration
//public class ChatGptServcieFactory {
//
//    @Resource
//    private ChatModel chatApiGptChatModel;
//
//    @Resource
//    private StreamingChatModel chatGPTStreamingChatModel;
//
//    @Resource
//    private ContentRetriever nomicContentRetriever;
//
//    @Resource
//    private McpToolProvider mcpToolProvider;
//
//    @Resource
//    private EmailSenderTool emailSenderTool;
//
//
//    @Bean
//    public ChatGptServcie chatGptServcie() {
//        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
//        return AiServices.builder(ChatGptServcie.class)
//                .chatModel(chatApiGptChatModel)
//                .streamingChatModel(chatGPTStreamingChatModel)
//                .chatMemory(chatMemory)
//                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
//                .contentRetriever(nomicContentRetriever)
//                .tools(new InterviewQuestionTool(),emailSenderTool)
//                .toolProvider(mcpToolProvider)
//                .build();
//    }
//
//
//
//}
