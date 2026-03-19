package com.hyj.aicodehelper.mcp;//package com.hyj.aicodehelper.mcp;
//
//import dev.langchain4j.mcp.McpToolProvider;
//import dev.langchain4j.mcp.client.DefaultMcpClient;
//import dev.langchain4j.mcp.client.McpClient;
//import dev.langchain4j.mcp.client.transport.McpTransport;
//import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * 配置 MCP 工具提供器 - 支持多个MCP服务器
// */
//@Slf4j
//@Configuration
//public class McpServerConfig {
//
//    private final McpServerProperties mcpServerProperties;
//
//    public McpServerConfig(McpServerProperties mcpServerProperties) {
//        this.mcpServerProperties = mcpServerProperties;
//    }
//
//    @Bean
//    public McpToolProvider mcpToolProvider() {
//        List<McpClient> mcpClients = new ArrayList<>();
//
//        // 添加现有的MCP客户端（保留原有配置）
//        McpClient existingClient = createMiniMaxMcpClient();
//        if (existingClient != null) {
//            mcpClients.add(existingClient);
//        }
//
//        // 添加配置的MCP服务器
//        if (mcpServerProperties != null && mcpServerProperties.getServers() != null) {
//            for (Map.Entry<String, McpServerProperties.McpServerConfig> entry :
//                    mcpServerProperties.getServers().entrySet()) {
//                try {
//                    McpClient client = createMcpClientFromConfig(entry.getKey(), entry.getValue());
//                    if (client != null) {
//                        mcpClients.add(client);
//                        log.info("MCP服务器 '{}' 配置成功", entry.getKey());
//                    }
//                } catch (Exception e) {
//                    log.error("配置MCP服务器 '{}' 失败: {}", entry.getKey(), e.getMessage());
//                }
//            }
//        }
//
//        if (mcpClients.isEmpty()) {
//            log.warn("没有配置任何MCP客户端");
//            return McpToolProvider.builder().build();
//        }
//
//        return McpToolProvider.builder()
//                .mcpClients(mcpClients.toArray(new McpClient[0]))
//                .build();
//    }
//
//    /**
//     * 创建原有的MiniMax MCP客户端（保留现有功能）
//     */
//    private McpClient createMiniMaxMcpClient() {
//        try {
//            // 这里可以保留原有的MiniMax配置逻辑
//            // 暂时返回null，因为原有的配置可能在其他地方
//            return null;
//        } catch (Exception e) {
//            log.warn("创建原有MiniMax MCP客户端失败: {}", e.getMessage());
//            return null;
//        }
//    }
//
//    /**
//     * 根据配置创建MCP客户端
//     */
//    private McpClient createMcpClientFromConfig(String serverName,
//                                                McpServerProperties.McpServerConfig config) {
//        try {
//            // 构建命令
//            String command = buildCommand(config);
//
//            // 创建传输层
//            McpTransport transport = StdioMcpTransport.builder()
//                    .command(List.of(command.split(" ")))
//                    .environment(config.getEnv())
//                    .build();
//
//            // 创建MCP客户端
//            return new DefaultMcpClient.Builder()
//                    .key(serverName + "McpClient")
//                    .transport(transport)
//                    .build();
//        } catch (Exception e) {
//            log.error("创建MCP客户端 '{}' 失败: {}", serverName, e.getMessage());
//            throw e;
//        }
//    }
//
//    /**
//     * 构建完整的命令
//     */
//    private String buildCommand(McpServerProperties.McpServerConfig config) {
//        StringBuilder commandBuilder = new StringBuilder(config.getCommand());
//
//        if (config.getArgs() != null) {
//            for (String arg : config.getArgs()) {
//                commandBuilder.append(" ").append(arg);
//            }
//        }
//
//        return commandBuilder.toString();
//    }
//}