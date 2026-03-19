package com.hyj.aicodehelper.model;

import lombok.Getter;

/**
 * 大模型类型枚举
 */
@Getter
public enum ModelType {
    QWEN("qwen", "qwen模型"),
    MINIMAX("minimax", "Minimax模型"),
    OPENAI("openai", "OpenAI模型"),
    DEEPSEEK("deepseek", "DeepSeek模型"),
    CLAUDE("claude", "Claude模型");

    private final String code;
    private final String description;

    ModelType(String code, String description) {
        this.code = code;
        this.description = description;
    }


    public static String fromName(String code) {
        for (ModelType type : values()) {
            if (type.code.equals(code)) {
                return type.getDescription();
            }
        }
        throw new IllegalArgumentException("未知的模型类型: " + code);
    }

    public static ModelType fromCode(String code) {
        for (ModelType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的模型类型: " + code);
    }
}