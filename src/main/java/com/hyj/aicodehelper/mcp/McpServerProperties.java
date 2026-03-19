package com.hyj.aicodehelper.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MCP服务器配置属性类
 * 用于从application.yml加载MCP服务器相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "mcp")
public class McpServerProperties {
    
    private Map<String, McpServerConfig> servers;

    @Data
    public static class McpServerConfig {
        private String command;
        private String[] args;
        private Map<String, String> env;
    }
}