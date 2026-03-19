package com.hyj.aicodehelper.service;//package com.hyj.aicodehelper.service;
//
//import dev.langchain4j.service.Result;
//import jakarta.annotation.Resource;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//class AiCodeHelperServcieTest {
//
//    @Resource
//    private AiCodeHelperServcie aiCodeHelperServcie;
//
//    @Test
//    void chat() {
//        String chat = aiCodeHelperServcie.chat("你好，我是胡英俊");
//        System.out.println(chat);
//        chat = aiCodeHelperServcie.chat("帮我回答一下spring的@Component 和 @Configuration区别");
//        System.out.println(chat);
////        chat = aiCodeHelperServcie.chat("我现在在江苏省吉安市");
////        System.out.println(chat);
////        chat = aiCodeHelperServcie.chat("我在哪来着？");
////        System.out.println(chat);
//    }
//
//    @Test
//    void chatReond() {
//        AiCodeHelperServcie.Reond reond = aiCodeHelperServcie.chatReond("你好，我是胡英俊，今年18岁零3个月了，请帮我制定一个学习计划");
//        System.out.println(reond.toString());
//    }
//
//    @Test
//    void chatRag() {
//        Result<String> chatRag = aiCodeHelperServcie.chatRag("第一次找工作会遇到什么问题？有哪些面试要点？");
//        System.out.println(chatRag.sources());
//        System.out.println(chatRag.content());
//    }
//
//    @Test
//    void chatWithTools() {
//        String result = aiCodeHelperServcie.chat("有哪些关于多线程的面试题？");
//        System.out.println(result);
//    }
//
//    @Test
//    void chatWithMcp() {
//        String result = aiCodeHelperServcie.chat("帮我介绍一下刘德华");
//        System.out.println("================================================结果=================================================");
//        System.out.println(result);
//    }
//
//    @Test
//    void chatWithGuardrail() {
//        String result = aiCodeHelperServcie.chat("kill the game");
//        System.out.println("================================================结果=================================================");
//        System.out.println(result);
//    }
//}