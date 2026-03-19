package com.hyj.aicodehelper.service.aiservice;


import com.hyj.aicodehelper.service.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

@InputGuardrails({SafeInputGuardrail.class})
public interface MinimaxServcie {

    //    @SystemMessage(fromResource = "System-prompt-biancheng.txt")
    @SystemMessage("""
            你是一个专业的天气助手，专门为中国城市提供天气信息。
            你可以查询当前天气、未来一周天气预报、天气指数和生活建议。
            请根据用户的问题选择合适的功能进行回答。
            """)
    Flux<String> chatStream(@MemoryId int memoryId, @UserMessage String userMessage);


    Flux<String> chatStreamImage(@MemoryId int memoryId, @UserMessage dev.langchain4j.data.message.UserMessage userMessage);

}
