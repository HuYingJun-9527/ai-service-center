package com.hyj.aicodehelper.service;

import com.hyj.aicodehelper.model.ModelType;
import com.hyj.aicodehelper.service.aiservice.AiCodeHelperServcie;
import com.hyj.aicodehelper.service.aiservice.DeepSeekServcie;
import com.hyj.aicodehelper.service.aiservice.MinimaxServcie;
import com.hyj.aicodehelper.manager.StreamStopManager;
import com.hyj.aicodehelper.service.model.ModelManagerService;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 增强的AI服务实现，支持多模型选择
 */
@Service
@Slf4j
public class EnhancedAiCodeHelperService {

    @Resource
    private ModelManagerService modelManagerService;

    @Resource
    private AiCodeHelperServcie aiCodeHelperServcie;

    @Resource
    private MinimaxServcie minimaxServcie;

    @Resource
    private DeepSeekServcie deepSeekServcie;

    @Resource
    private StreamStopManager streamStopManager;


    /**
     * 流式对话，支持多模型选择
     *
     * @param message   用户输入消息
     * @param memoryId  会话ID
     * @param modelCode 模型编码
     * @return 流式响应Flux，包含模型输出内容
     */
    public Flux<String> chatStreamd(String message, int memoryId, String modelCode) {
        // 创建可中断的Flux
        return Flux.create(sink -> {
            // 检查是否被停止
            Sinks.Many<String> stopSink = streamStopManager.getStopSink(memoryId);
            if (stopSink != null) {
                stopSink.asFlux().doOnComplete(() -> {
                            log.info("收到停止信号，中断流: memoryId={}", memoryId);
                            sink.complete();
                        })
                        .subscribe();
            }

            // 原有的流处理逻辑
            Flux<String> originalFlux = getOriginalFlux(message, memoryId, modelCode);

            originalFlux.subscribe(
                    chunk -> {
                        if (!sink.isCancelled()) {
                            sink.next(chunk);
                        }
                    },
                    error -> {
                        if (!sink.isCancelled()) {
                            sink.error(error);
                        }
                    },
                    () -> {
                        if (!sink.isCancelled()) {
                            sink.complete();
                        }
                    }
            );
        });
    }

    private Flux<String> getOriginalFlux(String message, int memoryId, String modelCode) {
        // 确保所有模型都使用相同的RAG和工具配置
        if (ModelType.MINIMAX.getCode().equals(modelCode)) {
            return minimaxServcie.chatStreamImage(memoryId, UserMessage.from(message));
        } else if (ModelType.QWEN.getCode().equals(modelCode)) {
            return aiCodeHelperServcie.chatStream(memoryId, message);
        } else if (ModelType.DEEPSEEK.getCode().equals(modelCode)) {
            return deepSeekServcie.chatStream(memoryId, message);
        } else {
            // 默认使用通义千问
            return minimaxServcie.chatStream(memoryId, message);
        }
    }

    /**
     * 图片流式对话，支持多模型选择
     *
     * @param images    图片文件列表
     * @param text      用户文本消息
     * @param memoryId  会话ID
     * @param modelCode 模型编码
     * @return 流式响应Flux，包含模型输出内容
     */
    public Flux<String> chatStreamWithImage(List<MultipartFile> images, String text, int memoryId, String modelCode) {
        // 创建可中断的Flux
        return Flux.create(sink -> {
            // 检查是否被停止
            Sinks.Many<String> stopSink = streamStopManager.getStopSink(memoryId);
            if (stopSink != null) {
                stopSink.asFlux().doOnComplete(() -> {
                            log.info("收到停止信号，中断图片处理流: memoryId={}", memoryId);
                            sink.complete();
                        })
                        .subscribe();
            }
            // 处理图片并构建用户消息
            try {
                UserMessage userMessage = buildUserMessageWithImages(images, text);
                // 调用原始图片处理流
                Flux<String> originalFlux = getOriginalImageFlux(userMessage, memoryId, modelCode);
                // 订阅原始图片处理流，将结果发送到Flux
                originalFlux.subscribe(
                        chunk -> {
                            if (!sink.isCancelled()) {
                                sink.next(chunk);
                            }
                        },
                        error -> {
                            if (!sink.isCancelled()) {
                                log.error("图片处理流错误: ", error);
                                sink.error(error);
                            }
                        },
                        () -> {
                            if (!sink.isCancelled()) {
                                sink.complete();
                            }
                        }
                );
            } catch (Exception e) {
                log.error("构建图片消息失败: ", e);
                sink.error(e);
            }
        });
    }

    /**
     * 构建包含图片的用户消息
     * @param images 图片文件列表
     * @param text   用户文本消息
     * @return 包含图片和文本的用户消息
     * @throws IOException 如果图片转换失败
     */
    private UserMessage buildUserMessageWithImages(List<MultipartFile> images, String text) throws IOException {
        List<Content> contents = new ArrayList<>();

        if (text != null && !text.trim().isEmpty()) {
            contents.add(TextContent.from(text));
        }

        for (MultipartFile image : images) {
            String base64Image = convertToBase64(image);
            String mimeType = getMimeType(image.getOriginalFilename());
            contents.add(ImageContent.from(base64Image, mimeType));
        }

        return UserMessage.from(contents);
    }

    /**
     * 将图片转换为Base64编码
     */
    private String convertToBase64(MultipartFile image) throws IOException {
        byte[] imageBytes = image.getBytes();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 根据文件名获取MIME类型
     */
    private String getMimeType(String filename) {
        if (filename == null) {
            return "image/jpeg";
        }
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFilename.endsWith(".webp")) {
            return "image/webp";
        } else {
            return "image/jpeg";
        }
    }

    /**
     * 根据模型获取对应的图片处理Flux
     */
    private Flux<String> getOriginalImageFlux(UserMessage userMessage, int memoryId, String modelCode) {
        if (ModelType.MINIMAX.getCode().equals(modelCode)) {
            return minimaxServcie.chatStreamImage(memoryId, userMessage);
        } else if (ModelType.QWEN.getCode().equals(modelCode)) {
            // 通义千问支持图片
            return aiCodeHelperServcie.chatStreamWithImage(memoryId, userMessage);
        } else if (ModelType.DEEPSEEK.getCode().equals(modelCode)) {
            // DeepSeek当前版本可能不支持图片，返回错误信息
            return Flux.error(new UnsupportedOperationException(
                    "当前模型 【" + modelCode + "】 不支持图片功能，请切换到通义千问或MiniMax模型"));
        } else {
            // 默认使用MiniMax
            return minimaxServcie.chatStreamImage(memoryId, userMessage);
        }
    }


    /**
     * 获取支持的模型列表
     */
    public ModelType[] getSupportedModels() {
        return modelManagerService.getSupportedModels();
    }
}