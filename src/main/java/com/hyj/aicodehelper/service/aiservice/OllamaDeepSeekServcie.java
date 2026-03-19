package com.hyj.aicodehelper.service.aiservice;


import com.hyj.aicodehelper.service.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

@InputGuardrails({SafeInputGuardrail.class})
public interface OllamaDeepSeekServcie {

    @SystemMessage(fromResource = "System-prompt-biancheng.txt")
    Flux<String> chatStream(@MemoryId int memoryId, @UserMessage String userMessage);

}
