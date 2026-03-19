package com.hyj.aicodehelper.controller;

import com.hyj.aicodehelper.model.ModelType;
import com.hyj.aicodehelper.service.aiservice.AiCodeHelperServcie;
import com.hyj.aicodehelper.service.EnhancedAiCodeHelperService;
import com.hyj.aicodehelper.manager.StreamStopManager;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {

    @Resource
    private AiCodeHelperServcie aiCodeHelperService;

    @Resource
    private EnhancedAiCodeHelperService enhancedAiCodeHelperService;

    // 添加停止管理器
    @Resource
    private StreamStopManager streamStopManager;

    @GetMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(int memoryId, String message, String modelCode) {
        return aiCodeHelperService.chatStream(memoryId, message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    @GetMapping("/chatsFlux")
    public Flux<ServerSentEvent<String>> chatsFlux(
            @RequestParam String message,
            @RequestParam String modelCode,
            @RequestParam int memoryId) {
        try {
            String streamId = UUID.randomUUID().toString();

            Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(30))
                    .map(tick -> ServerSentEvent.<String>builder()
                            .event("heartbeat")
                            .data("ping")
                            .build());

            return enhancedAiCodeHelperService.chatStreamd(message, memoryId, modelCode)
                    .doOnSubscribe(subscription -> streamStopManager.registerStream(streamId, memoryId))
                    .doOnTerminate(() -> streamStopManager.unregisterStream(streamId))
                    // 使用安全的类型转换
                    .map(this::convertChunkToString)
                    .map(chunk -> ServerSentEvent.<String>builder()
                            .data(chunk)
                            .build())
                    .concatWithValues(
                            ServerSentEvent.<String>builder()
                                    .event("end")
                                    .data("对话结束")
                                    .build()
                    )
                    .mergeWith(heartbeat)
                    .timeout(Duration.ofMinutes(10));
        } catch (Exception e) {
            return Flux.error(new IllegalArgumentException("对话失败: " + e.getMessage() +
                    "\n建议：1. 检查API Key是否正确 2. 确认账户余额充足 3. 尝试切换其他模型"));
        }
    }


    /**
     * 将响应块转换为字符串，处理LinkedHashMap类型
     */
    private String convertChunkToString(Object chunk) {
        if (chunk instanceof String) {
            // 如果已经是String，直接返回
            return (String) chunk;
        } else if (chunk instanceof LinkedHashMap<?, ?> map) {
            // 从LinkedHashMap中提取文本内容
            Object content = map.get("content");
            if (content != null) {
                return content.toString();
            }
            Object text = map.get("text");
            if (text != null) {
                return text.toString();
            }
            Object messageObj = map.get("message");
            if (messageObj != null) {
                return messageObj.toString();
            }
            // 如果都没有，返回整个对象的字符串表示
            return map.toString();
        } else {
            // 其他类型直接转换为字符串
            return chunk != null ? chunk.toString() : "";
        }
    }

    /**
     * 上传图片并生成响应流
     *
     * @param images    图片文件列表
     * @param text      用户文本消息
     * @param modelCode 模型编码
     * @param memoryId  内存ID
     * @return 包含图片和文本的用户消息
     */
    @PostMapping(value = "/uploadImage", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> uploadImage(
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam("modelCode") String modelCode,
            @RequestParam("memoryId") int memoryId) {
        try {
            // 参数验证
            if (images == null || images.isEmpty()) {
                return Flux.error(new IllegalArgumentException("请上传至少一张图片"));
            }
            // 验证图片文件
            for (MultipartFile image : images) {
                if (image.isEmpty()) {
                    return Flux.error(new IllegalArgumentException("存在空图片文件"));
                }
                String filename = image.getOriginalFilename();
                if (filename != null && !filename.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
                    return Flux.error(new IllegalArgumentException("不支持的图片格式: " + filename + "，仅支持 jpg、jpeg、png、gif、webp"));
                }
            }
            String streamId = UUID.randomUUID().toString();
            log.info("收到图片上传请求: images={}, text={}, modelCode={}, memoryId={}",
                    images.size(), text, modelCode, memoryId);
            // 添加心跳机制 每30秒发送一次心跳事件，保持连接活跃
            Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(30))
                    .map(tick -> ServerSentEvent.<String>builder()
                            .event("heartbeat")
                            .data("ping")
                            .build());
            return enhancedAiCodeHelperService.chatStreamWithImage(images, text, memoryId, modelCode)
                    .doOnSubscribe(subscription -> {
                        // 注册流ID到停止管理器
                        streamStopManager.registerStream(streamId, memoryId);
                    })
                    .doOnTerminate(() -> {
                        // 流结束时注销
                        streamStopManager.unregisterStream(streamId);
                        log.info("图片处理流结束: streamId={}, memoryId={}", streamId, memoryId);
                    })
                    .map(chunk -> ServerSentEvent.<String>builder()
                            .data(chunk)
                            .build())
                    .concatWith(Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("end")
                                    .data("处理结束")
                                    .build()
                    ))
                    .mergeWith(heartbeat.takeUntilOther(Flux.empty())) // 限制心跳只在流活跃时发送
                    .timeout(Duration.ofMinutes(10))
                    .onErrorResume(throwable -> {
                        // 错误时发送结束信号
                        log.error("图片处理流异常: ", throwable);
                        return Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("error")
                                        .data("处理失败: " + throwable.getMessage())
                                        .build(),
                                ServerSentEvent.<String>builder()
                                        .event("end")
                                        .data("流已结束")
                                        .build()
                        );
                    });
        } catch (Exception e) {
            log.error("图片处理失败: ", e);
            return Flux.error(new IllegalArgumentException("图片处理失败: " + e.getMessage() +
                    "\n建议：1. 检查API Key是否正确 2. 确认模型是否支持图片功能 3. 尝试切换其他模型"));
        }
    }

    /**
     * 停止AI输出接口
     */
    @PostMapping("/stopStream")
    public ResponseEntity<String> stopStream(@RequestParam int memoryId) {
        try {
            boolean stopped = streamStopManager.stopStream(memoryId);
            if (stopped) {
                return ResponseEntity.ok("AI输出已停止");
            } else {
                return ResponseEntity.ok("未找到对应的AI输出流");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("停止失败: " + e.getMessage());
        }
    }

    /**
     * 获取支持的模型列表
     */
    @GetMapping("/models")
    public ResponseEntity<List<ModelInfo>> getSupportedModels() {
        ModelType[] models = enhancedAiCodeHelperService.getSupportedModels();
        List<ModelInfo> modelInfos = Arrays.stream(models)
                .map(model -> new ModelInfo(model.getCode(), model.getDescription()))
                .toList();
        return ResponseEntity.ok(modelInfos);
    }

    /**
     * 模型信息DTO
     */
    @Setter
    @Getter
    public static class ModelInfo {
        private String code;
        private String description;

        public ModelInfo(String code, String description) {
            this.code = code;
            this.description = description;
        }

    }
}