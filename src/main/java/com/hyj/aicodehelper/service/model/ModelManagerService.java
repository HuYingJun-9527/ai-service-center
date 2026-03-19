package com.hyj.aicodehelper.service.model;

import com.hyj.aicodehelper.model.ModelType;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型管理服务
 */
@Service
@Slf4j
public class ModelManagerService {


    @Resource
    private StreamingChatModel deepSeekStreamingChatModel;

    @Resource
    private StreamingChatModel minimaxStreamingChatModel;


    private final Map<ModelType, StreamingChatModel> modelMap = new ConcurrentHashMap<>();

    /**
     * 初始化模型映射
     */
//    public void initModelMap() {
//        modelMap.put(ModelType.QWEN, qwenChatModel);
//        modelMap.put(ModelType.DEEPSEEK, deepSeekStreamingChatModel);
//        modelMap.put(ModelType.MINIMAX, minimaxStreamingChatModel);
//        log.info("模型映射初始化完成，当前支持模型: {}", modelMap.keySet());
//    }

    /**
     * 根据模型类型获取对应的ChatModel
     */
    public StreamingChatModel getChatModel(ModelType modelType) {
        StreamingChatModel model = modelMap.get(modelType);
        if (model == null) {
            throw new IllegalArgumentException("不支持的模型类型: " + modelType);
        }
        return model;
    }

    /**
     * 根据模型代码获取对应的ChatModel
     */
    public StreamingChatModel getChatModel(String modelCode) {
        ModelType modelType = ModelType.fromCode(modelCode);
        return getChatModel(modelType);
    }

    /**
     * 获取所有支持的模型类型
     */
    public ModelType[] getSupportedModels() {
        return ModelType.values();
    }
}