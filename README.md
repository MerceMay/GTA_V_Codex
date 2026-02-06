# GTA V Codex — 基于 Spring AI 的智能 RAG 知识库与 Agent 系统

> A production-grade AI-powered knowledge base and autonomous agent system built with **Spring Boot 3.5**, **Spring AI 1.1**, **Milvus** vector database, and the **ReAct** agent paradigm.

---

## 📋 项目简介 | Project Overview

GTA V Codex 是一个基于 Spring AI 框架的全栈智能应用，集成了 **RAG（检索增强生成）** 知识库和 **ReAct Agent** 自主决策系统。项目以 GTA V 游戏百科为领域知识，展示了从文档 ETL 流水线到自主多步骤工具调用的完整 AI 工程化实践。

### 两大核心模式

| 模式 | 说明 | 核心技术 |
|------|------|----------|
| **Chat + RAG 模式** | 基于向量检索的知识库问答，提供精准的游戏攻略信息 | Query Rewriting → Multi-Query Expansion → Vector Similarity Search → Contextual Augmentation |
| **Agent 模式 (ReAct)** | 自主多步骤推理 Agent，可调用 7 种工具完成复杂任务 | Thought → Action → Observation 循环，支持并行工具调用 |

---

## 🏗️ 技术架构 | Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          Vue.js 3 Frontend                               │
│                   (SSE Stream Parser + Markdown Renderer)                 │
└────────────────────────────────┬─────────────────────────────────────────┘
                                 │ SSE / REST
┌────────────────────────────────▼─────────────────────────────────────────┐
│                      Spring Boot 3.5 + Spring AI 1.1                     │
│  ┌─────────────┐  ┌──────────────────┐  ┌─────────────────────────────┐  │
│  │ AIController │  │   ChatClient +   │  │     ReAct Agent Engine     │  │
│  │  REST API    │  │    Advisors      │  │  (Thought→Action→Observe)  │  │
│  │ (Sync/SSE/  │  │ ┌──────────────┐ │  │  ┌───────────────────────┐ │  │
│  │  Flux/Agent/ │  │ │MemoryAdvisor│ │  │  │  7 Tool Callbacks     │ │  │
│  │  RAG)        │  │ │LoggerAdvisor│ │  │  │  + MCP Integration    │ │  │
│  │             │  │ │  RAGAdvisor  │ │  │  │  + ToolCallingManager │ │  │
│  └─────────────┘  │ └──────────────┘ │  │  └───────────────────────┘ │  │
│                    └──────────────────┘  └─────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────────────┐    │
│  │                    RAG ETL Pipeline                                │    │
│  │  DocumentLoader → TextSplitter → KeywordEnricher → VectorStore   │    │
│  │  QueryRewriter → MultiQueryExpander → SimilaritySearch → Augment │    │
│  └───────────────────────────────────────────────────────────────────┘    │
└──────────┬───────────────┬───────────────┬───────────────┬───────────────┘
           │               │               │               │
    ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐ ┌─────▼──────┐
    │   Milvus    │ │ PostgreSQL  │ │   MinIO     │ │  LLM API   │
    │ Vector DB   │ │ Chat Memory │ │  Storage    │ │ (DeepSeek/ │
    │ (Cosine,    │ │ (JDBC-based │ │             │ │  OpenAI/   │
    │  IVF_FLAT)  │ │  windowed)  │ │             │ │  Ollama)   │
    └─────────────┘ └─────────────┘ └─────────────┘ └────────────┘
```

### 技术栈 | Tech Stack

| 分类 | 技术 |
|------|------|
| **框架** | Spring Boot 3.5.9, Spring AI 1.1.2 (BOM) |
| **语言** | Java 21 |
| **LLM** | DeepSeek-V3.2 (via SiliconFlow), Ollama (本地 Embedding) |
| **向量数据库** | Milvus 2.6 (IVF_FLAT 索引, Cosine 相似度, 4096 维向量) |
| **关系数据库** | PostgreSQL 18 (对话记忆持久化) |
| **对象存储** | MinIO (Milvus 存储后端) |
| **Embedding** | Qwen3-Embedding:8b (4096 dimensions, via Ollama) |
| **文档解析** | Spring AI Markdown Document Reader |
| **Agent 协议** | MCP (Model Context Protocol) Client + Server |
| **PDF 生成** | iText 9.5 (支持中文字体) |
| **网络爬虫** | Jsoup 1.22 |
| **API 文档** | Knife4j + OpenAPI 3 (Swagger UI) |
| **前端** | Vue.js 3 + Vite (SSE 流式渲染 + Markdown/LaTeX) |
| **容器化** | Docker Compose (一键部署全部基础设施) |

---

## 🔍 技术深度分析 | Technical Deep-Dive

### 1. RAG 流水线实现 (Retrieval-Augmented Generation Pipeline)

本项目实现了完整的 Spring AI RAG 流水线，涵盖 **文档摄入 (ETL)** 和 **检索增强查询** 两大阶段。

#### 1.1 文档 ETL 流程

```
Markdown 文档 → DocumentLoader → TextSplitter → KeywordEnricher → Milvus VectorStore
```

| ETL 阶段 | 实现类 | 技术细节 |
|----------|--------|----------|
| **文档加载** | `AppDocumentLoader` | 使用 `MarkdownDocumentReader` 加载 `classpath:/documents/*.md`，配置排除代码块和引用块，自动注入 `status=active` 和 `game=gta_v` 元数据标签 |
| **文本分块** | `AppCustomTextSplitter` | 使用 `TokenTextSplitter`，1000 token/chunk，最小 100 字符，最小嵌入 50 token，最大 10000 chunk |
| **关键词富化** | `AppDocumentKeywordEnricher` | 利用 LLM 的 `KeywordMetadataEnricher` 为每个文档块提取 Top 5 关键词，写入文档元数据 |
| **向量存储** | `AppVectorStoreConfig` | 通过 `@ConditionalOnProperty` 控制初始化，执行 Load → Split → Enrich → Store 流水线 |

#### 1.2 检索增强查询流程

```
用户查询 → QueryRewriter → MultiQueryExpander (×3) → VectorStore SimilaritySearch → 去重 → ContextualAugmenter → LLM
```

| 检索阶段 | 实现类 | 技术细节 |
|----------|--------|----------|
| **查询改写** | `AppQueryRewriter` | 使用 `RewriteQueryTransformer`，通过 LLM 将用户口语化查询改写为检索友好的精确查询 |
| **多查询扩展** | `AppMultiQueryExpander` | 使用 `MultiQueryExpander` 将单一查询扩展为 3 个不同角度的查询变体，提高召回率 |
| **向量检索** | `AppRagCustomAdvisorFactory` | 自定义 `DocumentRetriever`：每个扩展查询检索 Top 5 文档，相似度阈值 0.5，`status=active` 过滤，自动去重 |
| **上下文增强** | `AppContextualQueryAugmenterFactory` | 构建 `ContextualQueryAugmenter`，空上下文时返回领域限定的友好提示 |

**解决的核心问题**：
- ✅ **召回率不足** → Multi-Query Expansion 从 3 个角度检索，显著提升召回率
- ✅ **查询语义偏差** → Query Rewriting 将口语化查询转为检索友好形式
- ✅ **无关文档噪声** → 元数据过滤 (`status=active`) + 相似度阈值 (0.5) 双重过滤
- ✅ **检索结果重复** → 跨查询结果自动去重 (`.distinct()`)

---

### 2. Agent 与 Function Calling

本项目实现了完整的 **ReAct (Reasoning and Acting)** Agent 框架，通过 Spring AI 的 Function Calling 机制将大模型与后端业务接口深度集成。

#### 2.1 Agent 架构层次

```
AIAgent (GTA V 专家 Agent，注册 7 种工具 + MCP 动态工具)
   └── ToolCallAgent (工具调用 Agent，管理 think/act 循环)
         └── ReActAgent (ReAct 抽象层，定义 think → act → step 模板)
               └── BaseAgent (基础框架：状态管理、迭代控制、SSE 流式输出)
```

#### 2.2 Function Calling 机制

| 特性 | 实现细节 |
|------|----------|
| **工具注册** | `ToolRegistration` 使用 `ToolCallbacks.from()` 声明式注册 7 种 `@Tool` 标注的工具类 |
| **外部工具执行** | `ToolCallingChatOptions.internalToolExecutionEnabled(false)` — 禁用框架内部自动执行，由 Agent 手动控制执行时机 |
| **工具执行管理** | `ToolCallingManager` 统一执行工具调用，自动维护对话历史 |
| **并行调用** | Agent 支持单步内多工具并行调用，由 `ToolCallingManager.executeToolCalls()` 统一处理 |
| **MCP 集成** | 通过 `ToolCallbackProvider` 动态加载 MCP Server 暴露的远程工具 (SSE + stdio 双模式) |
| **终止控制** | `TerminateAgentTool` 实现优雅停止，Agent 检测到此工具被调用后设置状态为 COMPLETED |

#### 2.3 7 种内置工具

| 工具 | 功能 | 关键实现 |
|------|------|----------|
| `WebSearchTool` | 互联网搜索 | Serper API，解析知识图谱 + 自然搜索结果 + 相关问题 |
| `WebCrawlerTool` | 网页内容提取 | Jsoup HTML 解析，提取可见文本内容 |
| `FileOperationTool` | 文件读写 | 基于 `WorkspaceManager` 的沙箱化文件操作 |
| `TerminalOperationTool` | 终端命令执行 | 跨平台 (Windows/Linux)，捕获 stdout/stderr |
| `PdfGeneratorTool` | PDF 生成 | iText 9.5，支持中文字体 (SimSun/YaHei) |
| `ResourceDownloadTool` | 资源下载 | HTTP/HTTPS 资源下载到工作空间 |
| `TerminateAgentTool` | Agent 终止 | 信号式终止，设置 AgentState.COMPLETED |

#### 2.4 多步骤决策流程

```
用户请求 → Agent 接收
    ↓
┌─── Iteration 1~20 ────────────────────────────────────┐
│  Think: LLM 分析当前状态，决定下一步动作               │
│    ↓                                                    │
│  有工具调用? ─── 否 ──→ 输出 Final Answer，结束         │
│    │ 是                                                 │
│    ↓                                                    │
│  Act: ToolCallingManager 执行工具 (支持并行)            │
│    ↓                                                    │
│  Observe: 工具结果写入对话历史，进入下一轮迭代           │
└─────────────────────────────────────────────────────────┘
    ↓
循环检测 (连续 3 次相同输出自动终止) / 最大迭代数限制 (20)
```

---

### 3. 工程化实践 (Engineering Practices)

#### 3.1 ChatClient 与 Advisor 链

本项目充分利用 Spring AI 的 **ChatClient + Advisor** 机制实现关注点分离：

| Advisor | 类型 | 功能 |
|---------|------|------|
| `MessageChatMemoryAdvisor` | 内置 | 基于 PostgreSQL JDBC 的对话记忆，滑动窗口 20 条消息，支持多会话隔离 (`conversationId`) |
| `MyLoggerAdvisor` | 自定义 | 实现 `CallAdvisor` + `StreamAdvisor` 双接口，拦截同步/流式请求，记录用户输入和模型输出 |
| `ReReadingAdvisor` | 自定义 | 实现 `BaseAdvisor`，在用户查询后追加"再读一遍问题"提示，利用 Re2 技术提升回答准确性 |
| `RetrievalAugmentationAdvisor` | RAG | 在 RAG 查询中动态注入，执行向量检索 + 上下文增强 |

#### 3.2 Prompt 工程

- **System Prompt 模板化**：使用 StringTemplate (`.st` 文件) 管理 System Prompt，从 `classpath:/prompts/system_prompt.st` 加载
- **ReAct Prompt 结构化**：Agent 的 System Prompt 和 Next Step Prompt 定义了严格的 `Thought → Action → Final Answer` 格式约束
- **Query Augmentation**：空上下文时的领域限定提示模板 (`AppContextualQueryAugmenterFactory`)

#### 3.3 配置化与可观测性

- **BOM 依赖管理**：使用 `spring-ai-bom` 统一管理所有 Spring AI 子模块版本
- **条件化初始化**：ETL 流水线通过 `@ConditionalOnProperty("rag.initialize-vector-store")` 控制，避免重复灌库
- **DEBUG 日志**：`org.springframework.ai` 包级别 DEBUG 日志，完整追踪 AI 调用链路
- **CORS 全局配置**：`CORSConfig` 实现 `WebMvcConfigurer`，支持跨域前后端分离开发
- **API 文档**：Knife4j + OpenAPI 3 自动生成可交互的 API 文档

---

### 4. 性能与稳定性 (Performance & Reliability)

#### 4.1 流式响应 (Streaming)

项目实现了 **5 种** 流式响应端点，从多维度解决 LLM 响应延迟问题：

| 端点 | 技术方案 | 适用场景 |
|------|----------|----------|
| `GET /ai/chat/sse` | `Flux<String>` + `text/event-stream` | 标准 SSE 流式响应 |
| `GET /ai/chat/sse/generic` | `Flux<ServerSentEvent<String>>` | 结构化 SSE，支持 event ID/type |
| `GET /ai/chat/sse/emitter` | `SseEmitter` (5 分钟超时) | Servlet 兼容的推送方案 |
| `GET /ai/chat/agent` | `SseEmitter` (10 分钟超时) + `CompletableFuture.runAsync` | Agent 异步流式输出 |
| `GET /ai/chat/rag/sse` | `Flux<String>` + `text/event-stream` | RAG 流式检索增强响应 |

#### 4.2 Agent 异常处理与稳定性机制

| 机制 | 实现 |
|------|------|
| **状态机** | `AgentState` 枚举 (IDLE → PROCESSING → COMPLETED/ERROR)，保证 Agent 生命周期可控 |
| **循环检测** | 连续 3 次相同输出自动终止，防止 LLM 陷入死循环 |
| **迭代上限** | 最大 20 次迭代，防止无限推理消耗资源 |
| **超时控制** | SseEmitter 超时回调 (`onTimeout`)，自动清理资源 |
| **资源清理** | `BaseAgent.cleanUp()` 在 `finally` 块中执行，重置迭代计数、清空消息列表、恢复 IDLE 状态 |
| **工作空间隔离** | `WorkspaceManager` 为每次运行创建独立临时目录，实现 `DisposableBean` 在 Bean 销毁时自动清理 |
| **异常传播** | 工具执行异常被捕获后通过 SseEmitter 发送错误信息，避免连接挂起 |

---

## 📝 简历核心要点 | Resume Highlights

> **项目名称**：基于 Spring AI 的智能 RAG 知识库与 Agent 系统
>
> **技术栈**：Spring Boot 3.5, Spring AI 1.1, Milvus, PostgreSQL, DeepSeek/Ollama, Flux/SSE, Docker Compose, MCP, iText, Vue.js 3

**核心要点：**

1. **针对** RAG 知识库检索准确率不足的挑战，**利用** Spring AI 的 `RewriteQueryTransformer` + `MultiQueryExpander` + 自定义 `DocumentRetriever` 特性，**实现了** 完整的 ETL 文档处理流水线（Markdown 解析 → Token 分块 → LLM 关键词富化 → Milvus 向量存储）和多查询扩展检索策略（单查询扩展为 3 路并行检索 + 相似度阈值过滤 + 元数据标签过滤 + 跨查询去重），**解决了** 单一查询语义偏差导致的低召回率问题，显著提升了知识库问答的精准度。

2. **针对** 大模型无法直接操作外部系统的挑战，**利用** Spring AI 的 `ToolCallingManager` + `@Tool` 声明式注册 + MCP 协议动态工具发现特性，**实现了** 基于 ReAct 范式的自主决策 Agent 系统（支持 Think → Action → Observation 多步推理循环、7 种工具并行调用、循环检测与优雅终止机制），**解决了** LLM 与后端业务系统（文件操作、网络搜索、PDF 生成、终端命令）深度集成的工程化难题。

3. **针对** LLM 响应延迟导致用户体验差的挑战，**利用** Spring AI 的 `ChatClient.stream()` + Spring WebFlux 的 `Flux<String>`/`ServerSentEvent` + `SseEmitter`/`CompletableFuture.runAsync` 异步非阻塞特性，**实现了** 5 种流式响应端点（标准 SSE / 结构化 SSE / Servlet Emitter / Agent 流式 / RAG 流式），并配合 `AgentState` 状态机、迭代上限、超时回调和 `WorkspaceManager` 资源隔离等稳定性保障机制，**解决了** 大模型长耗时推理场景下的首 Token 延迟和连接稳定性问题。

---

## 🛠️ 快速开始 | Getting Started

### 前置条件
- Java 21+
- Node.js 18+ & npm
- Docker & Docker Compose
- LLM API Key (DeepSeek/OpenAI/SiliconFlow)

### 1. 启动基础设施
```bash
docker-compose up -d
```
启动 PostgreSQL、Milvus、MinIO、Attu (Milvus UI)、前端等服务。

### 2. 配置后端
在 `src/main/resources/application.yml` 中配置 API 密钥：
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

首次运行时，如需初始化向量库，设置：
```yaml
rag:
  initialize-vector-store: true
```

### 3. 启动应用

**后端：**
```bash
./mvnw spring-boot:run
```

**前端：**
```bash
cd front-end
npm install
npm run dev
```

### 4. 访问服务

| 服务 | 地址 |
|------|------|
| API 文档 (Swagger) | http://localhost:8080/api/swagger-ui.html |
| 前端界面 | http://localhost:5173 |
| Milvus UI (Attu) | http://localhost:8000 |
| MinIO Console | http://localhost:9001 |

---

## 📡 API 端点 | API Endpoints

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/ai/chat/sync` | GET | 同步对话 |
| `/api/ai/chat/sse` | GET | SSE 流式对话 (Flux) |
| `/api/ai/chat/sse/generic` | GET | 结构化 SSE 对话 |
| `/api/ai/chat/sse/emitter` | GET | SseEmitter 流式对话 |
| `/api/ai/chat/rag` | GET | RAG 知识库问答 |
| `/api/ai/chat/rag/sse` | GET | RAG 流式知识库问答 |
| `/api/ai/chat/agent` | GET | Agent 模式 (ReAct + 工具调用) |
| `/api/health` | GET | 健康检查 |

---

## 📖 使用示例 | Usage

- **知识库问答 (RAG)**：询问 "What is the best approach for the Big Score heist?" 或 "哪里能找到所有 50 封信件碎片？"
- **Agent 任务执行**：在 Agent 模式下尝试 "Find 3 recent articles about AI news, summarize them, and save the result as a PDF."

---

## 📁 项目结构 | Project Structure

```
GTA_V_Codex/
├── src/main/java/com/mercemay/aiagent/
│   ├── AiAgentApplication.java          # Spring Boot 启动类
│   ├── advisor/                          # Advisor 拦截器
│   │   ├── MyLoggerAdvisor.java          #   日志记录 (Call + Stream)
│   │   └── ReReadingAdvisor.java         #   Re-Reading 准确性增强
│   ├── agent/                            # ReAct Agent 框架
│   │   ├── BaseAgent.java                #   基础框架 (状态机/流式/迭代)
│   │   ├── ReActAgent.java               #   ReAct 抽象层 (think/act)
│   │   ├── ToolCallAgent.java            #   工具调用 Agent
│   │   ├── AIAgent.java                  #   GTA V 专家 Agent
│   │   └── model/AgentState.java         #   状态枚举
│   ├── app/App.java                      # ChatClient 编排 (Chat/RAG/Tools/MCP)
│   ├── config/                           # 配置类
│   │   ├── ChatMemoryConfig.java         #   对话记忆 (PostgreSQL, 20 条窗口)
│   │   └── CORSConfig.java              #   CORS 跨域配置
│   ├── controller/                       # REST API 控制器
│   │   ├── AIController.java             #   AI 对话端点 (7 个端点)
│   │   └── HealthController.java         #   健康检查
│   ├── manager/WorkspaceManager.java     # 工作空间沙箱管理
│   ├── rag/                              # RAG 流水线组件
│   │   ├── AppDocumentLoader.java        #   Markdown 文档加载器
│   │   ├── AppCustomTextSplitter.java    #   Token 文本分块器
│   │   ├── AppDocumentKeywordEnricher.java #  LLM 关键词富化器
│   │   ├── AppVectorStoreConfig.java     #   向量库 ETL 配置
│   │   ├── AppQueryRewriter.java         #   查询改写器
│   │   ├── AppMultiQueryExpander.java    #   多查询扩展器
│   │   ├── AppRagCustomAdvisorFactory.java #  RAG Advisor 工厂
│   │   └── AppContextualQueryAugmenterFactory.java # 上下文增强器工厂
│   └── tools/                            # Agent 工具集
│       ├── ToolRegistration.java         #   声明式工具注册
│       ├── WebSearchTool.java            #   网络搜索
│       ├── WebCrawlerTool.java           #   网页爬虫
│       ├── FileOperationTool.java        #   文件操作
│       ├── TerminalOperationTool.java    #   终端操作
│       ├── PdfGeneratorTool.java         #   PDF 生成
│       ├── ResourceDownloadTool.java     #   资源下载
│       └── TerminateAgentTool.java       #   Agent 终止
├── src/main/resources/
│   ├── application.yml                   # 应用配置
│   ├── prompts/system_prompt.st          # System Prompt 模板
│   └── documents/*.md                    # GTA V 知识库文档 (7 篇)
├── ai-agent-mcp-server/                  # MCP Server 子模块
├── front-end/                            # Vue.js 3 前端
├── docker-compose.yml                    # 基础设施编排
└── pom.xml                               # Maven 构建配置
```

---

## 📝 License

This project is for educational and research purposes. All GTA V related content belongs to Rockstar Games.
