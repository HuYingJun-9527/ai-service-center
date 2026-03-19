package com.hyj.aicodehelper.service.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

import java.util.Set;

/**
 * 安全输出守卫rail - 检测模型输出是否包含敏感词
 */
public class SafeOutputGuardrail implements OutputGuardrail {

   private static final Set<String> sensitiveWords = Set.of("kill", "evil");

   /**
    * 检测用户输入是否安全
    */
   @Override
   public OutputGuardrailResult validate(AiMessage aiMessage) {
       // 获取用户输入并转换为小写以确保大小写不敏感
       String inputText = aiMessage.text();
       // 使用正则表达式分割输入文本为单词
       String[] words = inputText.split("\\W+");
       // 遍历所有单词，检查是否存在敏感词
       for (String word : words) {
           if (sensitiveWords.contains(word)) {
               return fatal("Sensitive word detected: " + word);
          }
      }
       return success();
  }
}