package com.hyj.aicodehelper.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * 外部工具服务 （备用）
 * 用于执行uvx等外部工具命令
 */
@Service
public class ExternalToolService {

    public String runUvxTool(String toolName, String args) throws Exception {
        // 构造命令，例如: uvx cowsay "Hello from Spring Boot!"
        String command = String.format("uvx %s %s", toolName, args);

        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.redirectErrorStream(true); // 合并标准错误和输出

        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("uvx 命令执行失败，退出码: " + exitCode + "\n输出: " + output);
        }

        return output.toString();
    }

    /**
     * 运行带有环境变量的uvx工具
     */
    public String runUvxToolWithEnv(String toolName, String args, Map<String, String> env) throws Exception {
        String command = String.format("uvx %s %s", toolName, args);

        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.redirectErrorStream(true);

        // 设置环境变量
        if (env != null) {
            Map<String, String> processEnv = processBuilder.environment();
            processEnv.putAll(env);
        }

        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("uvx 命令执行失败，退出码: " + exitCode + "\n输出: " + output);
        }

        return output.toString();
    }
}