# AI Code Helper 项目技术文档

## 1. 项目概述

| 属性 | 值 |
|------|-----|
| 项目名称 | ai-code-helper |
| 项目版本 | 0.0.1-SNAPSHOT |
| Spring Boot版本 | 3.5.8 |
| Java版本 | 21 |
| 项目描述 | AI编程助手 |

## 2. 技术栈

### 2.1 核心框架
- **Spring Boot 3.5.8** - 核心应用框架
- **Spring Web** - Web服务
- **Spring Data Redis** - Redis数据访问
- **Lombok** - 简化代码

### 2.2 AI集成
- **LangChain4j 1.10.0** - AI模型集成框架
  - `langchain4j-community-dashscope-spring-boot-starter` - 通义千问集成
  - `langchain4j-open-ai` - OpenAI集成
  - `langchain4j-ollama` - Ollama本地模型集成
  - `langchain4j-mcp` - MCP协议支持
  - `langchain4j-qdrant` - 向量数据库支持
  - `langchain4j-reactor` - 响应式支持
- **AgentScope 1.0.7** - 多代理框架
  - `agentscope` - 核心包
  - `agentscope-extensions-mem0` - 内存扩展
  - `agentscope-spring-boot-starter` - Spring Boot集成
- **OpenAI Java SDK 4.15.0** - OpenAI API客户端

### 2.3 文档处理
- **Apache POI 5.2.5** - Word/Excel文档处理
- **Apache PDFBox** - PDF文档解析
- **Jsoup 1.20.1** - HTML解析

### 2.4 API文档
- **SpringDoc OpenAPI 2.7.0** - OpenAPI 3文档生成
- **Knife4j 4.5.0** - Swagger增强文档

### 2.5 其他
- **Jackson** - JSON处理
- **Reactor Core** - 响应式编程
- **Java Mail** - 邮件服务

## 3. 项目结构

```
src/main/java/com/hyj/aicodehelper/
├── AiCodeHelperApplication.java          # 应用入口
├── config/                               # 配置类
│   ├── CorsConfig.java                  # 跨域配置
│   ├── MultipartConfig.java             # 文件上传配置（最大50MB）
│   ├── RedisConfig.java                 # Redis配置
│   └── SwaggerConfig.java               # API文档配置
├── controller/                           # 控制器层
│   ├── AiController.java                # AI对话接口
│   ├── CityDataController.java          # 城市数据接口
│   ├── RagDataController.java           # RAG数据管理接口
│   └── RedisEmbeddingTestController.java # Redis嵌入测试接口
├── factory/                              # 工厂类
│   ├── AiCodeHelperServcieFactory.java  # AI服务工厂
│   ├── ChatGptServcieFactory.java      # ChatGPT服务工厂
│   ├── DeepseekServcieFactory.java     # DeepSeek服务工厂
│   └── MinimaxServcieFactory.java      # MiniMax服务工厂
├── init/                                 # 初始化配置
│   ├── ChatGptModelConfig.java          # ChatGPT模型配置
│   ├── DeepSeekChatModelConfig.java    # DeepSeek模型配置
│   ├── MinimaxChatModelConfig.java     # MiniMax模型配置
│   ├── OllamaChatModelConfig.java      # Ollama模型配置
│   └── QwenChatModelConfig.java        # 通义千问模型配置
├── listener/                             # 监听器
│   ├── MyConfiguration.java            # ChatModel监听器配置
│   └── RedisConnectionTest.java        # Redis连接测试
├── manager/                              # 管理器
│   ├── McpServerManager.java           # MCP服务器管理
│   └── StreamStopManager.java          # 流停止管理
├── mcp/                                   # MCP相关
│   ├── McpConfig.java                  # MCP配置
│   ├── McpServerConfig.java            # MCP服务器配置
│   └── McpServerProperties.java        # MCP服务器属性
├── model/                                # 数据模型
│   └── ModelType.java                  # 模型类型枚举
├── rag/                                   # RAG相关
│   ├── NomicRagConfig.java             # Nomic RAG配置
│   ├── RagConfig.java                  # RAG配置（已注释）
│   ├── config/
│   │   └── DocumentPreprocessor.java   # 文档预处理器
│   ├── service/
│   │   ├── RagDataManagementService.java       # RAG数据管理服务
│   │   └── RedisEmbeddingManagementService.java # Redis嵌入管理服务
│   └── store/
│       └── RedisEmbeddingStore.java     # Redis向量存储
├── service/                              # 服务层
│   ├── AiCodeHelper.java               # AI助手主服务
│   ├── EnhancedAiCodeHelperService.java # 增强AI服务（多模型支持）
│   ├── ExternalToolService.java        # 外部工具服务
│   ├── aiservice/                       # AI服务实现
│   │   ├── AiCodeHelperServcie.java   # 通义千问服务
│   │   ├── ChatGptServcie.java        # ChatGPT服务
│   │   ├── DeepSeekServcie.java        # DeepSeek服务
│   │   ├── MinimaxServcie.java         # MiniMax服务
│   │   └── OllamaDeepSeekServcie.java # Ollama DeepSeek服务
│   ├── guardrail/                       # 安全守卫
│   │   ├── SafeInputGuardrail.java     # 输入安全守卫
│   │   └── SafeOutputGuardrail.java   # 输出安全守卫
│   └── model/
│       └── ModelManagerService.java     # 模型管理服务
├── tools/                                # 工具类
│   ├── CityDataService.java           # 城市数据服务
│   ├── EmailSenderTool.java            # 邮件发送工具
│   ├── InterviewQuestionTool.java      # 面试题工具
│   └── WeatherWebTool.java            # 天气查询工具
└── utils/                               # 工具类
    └── RedisUtil.java                  # Redis工具类
```

## 4. 核心功能

### 4.1 AI对话服务

#### 接口列表

| 接口路径 | 方法 | 功能 |
|---------|------|------|
| `/ai/chat` | GET | 流式对话 |
| `/ai/chatsFlux` | GET | 多模型流式对话 |
| `/ai/uploadImage` | POST | 图片上传与处理 |
| `/ai/stopStream` | POST | 停止AI输出 |
| `/ai/models` | GET | 获取支持的模型列表 |

#### 支持的模型

| 模型代码 | 模型名称 |
|---------|---------|
| `qwen` | 通义千问 |
| `minimax` | MiniMax |
| `deepseek` | DeepSeek |
| `openai` | OpenAI |
| `claude` | Claude |

### 4.2 RAG数据管理

| 接口路径 | 方法 | 功能 |
|---------|------|------|
| `/api/rag/upload` | POST | 文件上传与处理 |
| `/api/rag/segments` | GET | 查询切片数据 |
| `/api/rag/supported-types` | GET | 获取支持的文件类型 |
| `/api/rag/stored-types` | GET | 获取已存储的类型 |
| `/api/rag/health` | GET | 健康检查 |

### 4.3 安全特性

#### 输入/输出安全守卫
- `SafeInputGuardrail` - 检测用户输入中的敏感词
- `SafeOutputGuardrail` - 检测AI输出中的敏感词
- 敏感词库：`kill`, `evil`（可扩展）

### 4.4 流式输出控制
- `StreamStopManager` - 管理流式输出的生命周期
- 支持通过 `memoryId` 停止指定的AI输出流
- 心跳机制：每30秒发送一次心跳保持连接

### 4.5 MCP工具集成
- 支持连接外部MCP服务器（如 bigmodel）
- 提供Web搜索等外部工具能力

## 5. 配置说明

### 5.1 文件上传配置
```yaml
spring.servlet.multipart:
  max-file-size: 50MB      # 单个文件最大50MB
  max-request-size: 100MB  # 请求最大100MB
```

### 5.2 Redis配置
- Key序列化：StringRedisSerializer
- Value序列化：Jackson2JsonRedisSerializer
- 支持常见数据类型：String、Hash、Set、List

### 5.3 CORS配置
- 允许所有来源（`allowedOriginPatterns: "*"`）
- 允许的HTTP方法：GET, POST, PUT, DELETE, OPTIONS
- 允许所有请求头

### 5.4 API文档
- Swagger UI地址：`/swagger-ui.html`
- Knife4j地址：`/doc.html`

## 6. 依赖版本汇总

| 依赖 | 版本 |
|-----|------|
| spring-boot-starter-parent | 3.5.8 |
| java.version | 21 |
| springdoc-openapi-starter-webmvc-ui | 2.7.0 |
| knife4j-openapi3-spring-boot-starter | 4.5.0 |
| langchain4j | 1.10.0 |
| langchain4j-community-dashscope-spring-boot-starter | 1.10.0-beta18 |
| langchain4j-open-ai | 1.10.0 |
| langchain4j-ollama | 1.10.0 |
| langchain4j-mcp | 1.10.0-beta18 |
| langchain4j-qdrant | 1.10.0-beta18 |
| langchain4j-reactor | 1.10.0-beta18 |
| agentscope | 1.0.7 |
| agentscope-extensions-mem0 | 1.0.7 |
| agentscope-spring-boot-starter | 1.0.7 |
| openai-java | 4.15.0 |
| poi | 5.2.5 |
| poi-ooxml | 5.2.5 |
| jsoup | 1.20.1 |
| lettuce-core | 6.5.3 |

## 7. 构建与运行

### 7.1 构建项目
```bash
./mvnw clean package
```

### 7.2 运行项目
```bash
./mvnw spring-boot:run
```

### 7.3 访问API文档
- Swagger UI: http://localhost:8080/swagger-ui.html
- Knife4j: http://localhost:8080/doc.html

## 8. 资源文件

项目包含以下资源文档：
- `docs-1/旅拍Vlog类.md` - 旅拍类文案模板
- `docs-1/美食探店类.md` - 美食探店类文案模板
- `docs/程序员常见面试题.md` - 程序员面试题库
- `docse/Java 编程学习路线.md` - Java学习路线
- `docs-qwen/天气助手.md` - 天气助手 prompt
- `test.json` / `test01.json` - 测试数据
