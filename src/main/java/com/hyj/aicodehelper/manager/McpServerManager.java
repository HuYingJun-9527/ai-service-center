package com.hyj.aicodehelper.manager;

import com.hyj.aicodehelper.mcp.McpServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * MCP服务器管理器
 * 负责管理和配置所有MCP服务器实例
 */
@Service
@Slf4j
public class McpServerManager {

    @Autowired(required = false)
    private McpServerProperties mcpServerProperties;

    /**
     * 获取所有配置的MCP服务器
     */
    public Map<String, McpServerProperties.McpServerConfig> getMcpServers() {
        if (mcpServerProperties == null || mcpServerProperties.getServers() == null) {
            return Map.of();
        }
        return mcpServerProperties.getServers();
    }

    /**
     * 获取特定MCP服务器的配置
     */
    public McpServerProperties.McpServerConfig getMcpServer(String serverName) {
        if (mcpServerProperties == null || mcpServerProperties.getServers() == null) {
            return null;
        }
        return mcpServerProperties.getServers().get(serverName);
    }

    /**
     * 检查MCP服务器是否已配置
     */
    public boolean hasMcpServer(String serverName) {
        return getMcpServer(serverName) != null;
    }
}